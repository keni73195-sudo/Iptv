package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.model.Channel
import com.example.model.PlaybackStatus
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class IptvPlayerManager(context: Context) {

  private data class ProbeResult(
    val finalUrl: String,
    val mimeType: String?,
    val failureMessage: String? = null,
    val failureCode: String = "",
  )

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var prepareJob: Job? = null

  val player: ExoPlayer =
    ExoPlayer.Builder(context.applicationContext)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
          .build(),
        true,
      )
      .setHandleAudioBecomingNoisy(true)
      .build()

  private val _playbackStatus = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
  val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val _videoResolutionLabel = MutableStateFlow<String?>(null)
  val videoResolutionLabel: StateFlow<String?> = _videoResolutionLabel.asStateFlow()

  private var currentChannel: Channel? = null

  /**
   * Optional handler invoked when a stream fails. Returns true if failover to a backup stream
   * was triggered (so no error banner should be shown yet).
   */
  var onStreamFailure: ((Channel) -> Boolean)? = null

  private val playerListener =
    object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        updateStatusFromPlayer()
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateStatusFromPlayer()
      }

      override fun onPlayerError(error: PlaybackException) {
        val failedChannel = currentChannel
        if (failedChannel != null && onStreamFailure?.invoke(failedChannel) == true) {
          return
        }
        val codeName = error.errorCodeName
        val detail =
          when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
              "Impossible de joindre le serveur de diffusion."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
              "Flux temporairement indisponible ($codeName)."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
              "Format du flux vidéo non pris en charge."
            else -> error.localizedMessage ?: "Erreur de lecture ($codeName)"
          }
        _playbackStatus.value = PlaybackStatus.Error(message = detail, codeName = codeName)
      }

      override fun onVideoSizeChanged(videoSize: VideoSize) {
        if (videoSize.width > 0 && videoSize.height > 0) {
          _videoResolutionLabel.value = "${videoSize.height}p"
        }
      }
    }

  init {
    player.addListener(playerListener)
  }

  fun playChannel(channel: Channel) {
    prepareJob?.cancel()
    currentChannel = channel
    player.stop()
    _videoResolutionLabel.value = channel.resolution
    _playbackStatus.value = PlaybackStatus.Buffering

    val userAgent = channel.userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT
    val headers = buildMap {
      put("User-Agent", userAgent)
      channel.referrer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
    }

    prepareJob =
      scope.launch {
        val probe = withContext(Dispatchers.IO) { probeStream(channel.streamUrl, headers) }
        ensureActive()
        if (currentChannel?.id != channel.id) return@launch

        if (probe.failureMessage != null) {
          if (onStreamFailure?.invoke(channel) == true) {
            return@launch
          }
          _playbackStatus.value =
            PlaybackStatus.Error(message = probe.failureMessage, codeName = probe.failureCode)
          return@launch
        }

        val dataSourceFactory =
          DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(headers)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        val detectedMimeType = probe.mimeType
        val mediaItemBuilder = MediaItem.Builder().setUri(probe.finalUrl)
        if (detectedMimeType != null) {
          mediaItemBuilder.setMimeType(detectedMimeType)
        }
        val mediaItem = mediaItemBuilder.build()

        val mediaSource =
          when (detectedMimeType) {
            MimeTypes.APPLICATION_M3U8 ->
              HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
            MimeTypes.APPLICATION_MPD ->
              DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            else -> DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
          }

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
      }
  }

  private fun probeStream(url: String, headers: Map<String, String>): ProbeResult {
    val cleanUrl = url.substringBefore("#origine", missingDelimiterValue = url).trim()
    var currentUrl = cleanUrl
    var hops = 0

    return try {
      while (true) {
        val parsedUrl =
          try {
            URL(currentUrl)
          } catch (_: MalformedURLException) {
            return ProbeResult(
              finalUrl = currentUrl,
              mimeType = null,
              failureMessage = "Adresse de flux invalide.",
              failureCode = "INVALID_URL",
            )
          }

        val connection =
          (parsedUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 6_000
            instanceFollowRedirects = true
            setRequestProperty("Connection", "close")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
          }

        try {
          val code = connection.responseCode
          if (code in 300..399 && hops < 5) {
            val location = connection.getHeaderField("Location")
            if (!location.isNullOrBlank()) {
              currentUrl = URL(parsedUrl, location).toString()
              hops++
              continue
            }
          }

          val finalUrl = connection.url?.toString() ?: currentUrl
          if (code !in 200..299) {
            val msg =
              when (code) {
                404, 410 -> "Flux hors ligne ou introuvable (HTTP $code)."
                401, 403 -> "Accès au flux refusé (HTTP $code)."
                else -> "Serveur indisponible (HTTP $code)."
              }
            return ProbeResult(
              finalUrl = finalUrl,
              mimeType = null,
              failureMessage = msg,
              failureCode = "HTTP_$code",
            )
          }

          val contentType = connection.contentType?.lowercase().orEmpty()
          if (contentType.contains("text/html")) {
            return ProbeResult(
              finalUrl = finalUrl,
              mimeType = null,
              failureMessage = "Lien vidéo invalide (page HTML reçue).",
              failureCode = "INVALID_CONTENT_TYPE",
            )
          }

          var mimeType = detectMimeType(finalUrl, contentType)
          if (mimeType == null) {
            val buffer = ByteArray(256)
            val bytesRead = connection.inputStream.use { it.read(buffer) }
            if (bytesRead > 0) {
              val preview = String(buffer, 0, bytesRead, Charsets.UTF_8).trimStart()
              mimeType =
                when {
                  preview.startsWith("#EXTM3U", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                  preview.contains("<MPD", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                  preview.startsWith("<!DOCTYPE html", ignoreCase = true) ||
                    preview.startsWith("<html", ignoreCase = true) ->
                    return ProbeResult(
                      finalUrl = finalUrl,
                      mimeType = null,
                      failureMessage = "Flux vidéo indisponible.",
                      failureCode = "INVALID_STREAM_PAYLOAD",
                    )
                  else -> null
                }
            }
          }

          return ProbeResult(finalUrl = finalUrl, mimeType = mimeType)
        } finally {
          connection.disconnect()
        }
      }
      @Suppress("UNREACHABLE_CODE")
      ProbeResult(finalUrl = cleanUrl, mimeType = detectMimeType(cleanUrl, ""))
    } catch (_: IOException) {
      ProbeResult(
        finalUrl = cleanUrl,
        mimeType = detectMimeType(cleanUrl, ""),
        failureMessage = "Impossible de joindre le serveur de diffusion.",
        failureCode = "IO_NETWORK_ERROR",
      )
    } catch (_: Exception) {
      ProbeResult(
        finalUrl = cleanUrl,
        mimeType = detectMimeType(cleanUrl, ""),
        failureMessage = "Erreur d'ouverture du flux.",
        failureCode = "STREAM_PROBE_ERROR",
      )
    }
  }

  private fun detectMimeType(url: String, contentType: String): String? =
    when {
      url.contains(".m3u8", ignoreCase = true) ||
        contentType.contains("mpegurl") -> MimeTypes.APPLICATION_M3U8
      url.contains(".mpd", ignoreCase = true) ||
        contentType.contains("dash+xml") -> MimeTypes.APPLICATION_MPD
      else -> null
    }

  fun retryCurrentChannel() {
    currentChannel?.let { playChannel(it) }
  }

  fun togglePlayPause() {
    if (player.playbackState == Player.STATE_IDLE && prepareJob?.isActive != true) {
      retryCurrentChannel()
      return
    }
    if (player.isPlaying || player.playWhenReady) {
      player.pause()
    } else {
      player.play()
    }
    updateStatusFromPlayer()
  }

  fun pause() {
    if (player.isPlaying) {
      player.pause()
      updateStatusFromPlayer()
    }
  }

  fun resumeIfReady() {
    if (currentChannel != null && player.playbackState != Player.STATE_IDLE) {
      player.playWhenReady = true
      updateStatusFromPlayer()
    }
  }

  fun toggleMute() {
    val muted = !_isMuted.value
    _isMuted.value = muted
    player.volume = if (muted) 0f else 1f
  }

  fun release() {
    prepareJob?.cancel()
    scope.cancel()
    player.removeListener(playerListener)
    player.release()
  }

  private fun updateStatusFromPlayer() {
    if (prepareJob?.isActive == true && player.playbackState == Player.STATE_IDLE) return
    if (_playbackStatus.value is PlaybackStatus.Error && player.playerError != null) return

    _playbackStatus.value =
      when (player.playbackState) {
        Player.STATE_IDLE -> PlaybackStatus.Idle
        Player.STATE_BUFFERING -> PlaybackStatus.Buffering
        Player.STATE_READY ->
          if (player.isPlaying || player.playWhenReady) {
            PlaybackStatus.Playing
          } else {
            PlaybackStatus.Paused
          }
        Player.STATE_ENDED -> PlaybackStatus.Paused
        else -> PlaybackStatus.Idle
      }
  }

  private companion object {
    const val DEFAULT_USER_AGENT =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
  }
}
