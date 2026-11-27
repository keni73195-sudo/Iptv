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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.model.Channel
import com.example.model.PlaybackStatus
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
  private var reconnectJob: Job? = null
  private var bufferingWatchdogJob: Job? = null
  private var steadyResetJob: Job? = null

  private class DiscontinuityTolerantAudioSink(
    private val delegate: AudioSink,
  ) : ForwardingAudioSink(delegate) {
    override fun setListener(listener: AudioSink.Listener) {
      super.setListener(
        object : AudioSink.Listener by listener {
          override fun onAudioSinkError(audioSinkError: Exception) {
            if (audioSinkError is AudioSink.UnexpectedDiscontinuityException) {
              delegate.handleDiscontinuity()
              return
            }
            listener.onAudioSinkError(audioSinkError)
          }
        }
      )
    }

    override fun handleBuffer(
      buffer: ByteBuffer,
      presentationTimeUs: Long,
      encodedAccessUnitCount: Int,
    ): Boolean =
      try {
        super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
      } catch (_: AudioSink.UnexpectedDiscontinuityException) {
        delegate.handleDiscontinuity()
        try {
          super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        } catch (_: AudioSink.UnexpectedDiscontinuityException) {
          delegate.flush()
          super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        }
      }
  }

  private val renderersFactory =
    object : DefaultRenderersFactory(context.applicationContext) {
      override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
      ): AudioSink? {
        val delegate =
          super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
            ?: return null
        return DiscontinuityTolerantAudioSink(delegate)
      }
    }

  val player: ExoPlayer =
    ExoPlayer.Builder(context.applicationContext, renderersFactory)
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
  private var hasStartedPlayingCurrentChannel = false
  private var autoReconnectAttempt = 0
  private var isUserPaused = false

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
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW && !isUserPaused) {
          player.seekToDefaultPosition()
          player.prepare()
          player.playWhenReady = true
          return
        }

        val failedChannel = currentChannel
        val isRecoverableAudioDiscontinuity =
          hasCause<AudioSink.UnexpectedDiscontinuityException>(error) ||
            error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
        if (failedChannel != null &&
          !isUserPaused &&
          (hasStartedPlayingCurrentChannel || isRecoverableAudioDiscontinuity) &&
          tryScheduleAutoReconnect(failedChannel)
        ) {
          return
        }

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
    cancelBackgroundTimers()
    currentChannel = channel
    hasStartedPlayingCurrentChannel = false
    autoReconnectAttempt = 0
    isUserPaused = false
    _videoResolutionLabel.value = channel.resolution
    _playbackStatus.value = PlaybackStatus.Buffering
    launchChannelPreparation(channel, isAutoReconnect = false)
  }

  private fun launchChannelPreparation(channel: Channel, isAutoReconnect: Boolean) {
    prepareJob?.cancel()
    player.stop()

    val userAgent = channel.userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT
    val headers = buildMap {
      put("User-Agent", userAgent)
      channel.referrer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
    }

    prepareJob =
      scope.launch {
        val probe = withContext(Dispatchers.IO) { probeStream(channel.streamUrl, headers) }
        ensureActive()
        if (currentChannel?.id != channel.id || isUserPaused) return@launch

        if (probe.failureMessage != null) {
          if ((hasStartedPlayingCurrentChannel || isAutoReconnect) &&
            tryScheduleAutoReconnect(channel)
          ) {
            return@launch
          }
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
            .setReadTimeoutMs(15_000)
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

  private fun tryScheduleAutoReconnect(channel: Channel): Boolean {
    if (isUserPaused || currentChannel?.id != channel.id) return false
    if (autoReconnectAttempt >= MAX_AUTO_RECONNECT_ATTEMPTS) return false

    autoReconnectAttempt++
    val attempt = autoReconnectAttempt
    bufferingWatchdogJob?.cancel()
    steadyResetJob?.cancel()
    reconnectJob?.cancel()
    _playbackStatus.value =
      PlaybackStatus.Reconnecting(attempt = attempt, maxAttempts = MAX_AUTO_RECONNECT_ATTEMPTS)

    reconnectJob =
      scope.launch {
        delay(AUTO_RECONNECT_BASE_DELAY_MS * attempt)
        ensureActive()
        if (currentChannel?.id == channel.id && !isUserPaused) {
          launchChannelPreparation(channel, isAutoReconnect = true)
        }
      }
    return true
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
    if (player.playbackState == Player.STATE_IDLE &&
      prepareJob?.isActive != true &&
      reconnectJob?.isActive != true
    ) {
      retryCurrentChannel()
      return
    }
    if (player.isPlaying || player.playWhenReady) {
      pause()
    } else {
      isUserPaused = false
      player.play()
      updateStatusFromPlayer()
    }
  }

  fun pause() {
    isUserPaused = true
    reconnectJob?.cancel()
    bufferingWatchdogJob?.cancel()
    steadyResetJob?.cancel()
    if (player.isPlaying || player.playWhenReady) {
      player.pause()
      updateStatusFromPlayer()
    }
  }

  fun resumeIfReady() {
    val active = currentChannel ?: return
    isUserPaused = false
    if (player.playbackState == Player.STATE_IDLE ||
      player.playbackState == Player.STATE_ENDED ||
      player.playerError != null
    ) {
      playChannel(active)
    } else {
      player.playWhenReady = true
      updateStatusFromPlayer()
    }
  }

  fun toggleMute() {
    val muted = !_isMuted.value
    _isMuted.value = muted
    player.volume = if (muted) 0f else 1f
  }

  fun stopPlaybackAndClear() {
    isUserPaused = true
    cancelBackgroundTimers()
    currentChannel = null
    hasStartedPlayingCurrentChannel = false
    autoReconnectAttempt = 0
    _videoResolutionLabel.value = null
    player.stop()
    player.clearMediaItems()
    _playbackStatus.value = PlaybackStatus.Idle
  }

  fun release() {
    cancelBackgroundTimers()
    scope.cancel()
    player.removeListener(playerListener)
    player.release()
  }

  private fun cancelBackgroundTimers() {
    prepareJob?.cancel()
    reconnectJob?.cancel()
    bufferingWatchdogJob?.cancel()
    steadyResetJob?.cancel()
  }

  private fun updateStatusFromPlayer() {
    if (reconnectJob?.isActive == true) return
    if (prepareJob?.isActive == true && player.playbackState == Player.STATE_IDLE) return
    if (_playbackStatus.value is PlaybackStatus.Error && player.playerError != null) return

    when (player.playbackState) {
      Player.STATE_IDLE -> {
        bufferingWatchdogJob?.cancel()
        _playbackStatus.value = PlaybackStatus.Idle
      }
      Player.STATE_BUFFERING -> {
        steadyResetJob?.cancel()
        if (autoReconnectAttempt > 0) {
          _playbackStatus.value =
            PlaybackStatus.Reconnecting(autoReconnectAttempt, MAX_AUTO_RECONNECT_ATTEMPTS)
        } else {
          _playbackStatus.value = PlaybackStatus.Buffering
        }
        startBufferingWatchdogIfNeeded()
      }
      Player.STATE_READY -> {
        bufferingWatchdogJob?.cancel()
        if (player.isPlaying || player.playWhenReady) {
          hasStartedPlayingCurrentChannel = true
          _playbackStatus.value = PlaybackStatus.Playing
          scheduleSteadyAttemptReset()
        } else {
          steadyResetJob?.cancel()
          _playbackStatus.value = PlaybackStatus.Paused
        }
      }
      Player.STATE_ENDED -> {
        bufferingWatchdogJob?.cancel()
        steadyResetJob?.cancel()
        val active = currentChannel
        if (!isUserPaused && hasStartedPlayingCurrentChannel && active != null &&
          tryScheduleAutoReconnect(active)
        ) {
          return
        }
        _playbackStatus.value = PlaybackStatus.Paused
      }
      else -> {
        bufferingWatchdogJob?.cancel()
        _playbackStatus.value = PlaybackStatus.Idle
      }
    }
  }

  private fun startBufferingWatchdogIfNeeded() {
    if (!hasStartedPlayingCurrentChannel || isUserPaused) return
    val channel = currentChannel ?: return
    if (bufferingWatchdogJob?.isActive == true) return

    bufferingWatchdogJob =
      scope.launch {
        delay(BUFFERING_STALL_TIMEOUT_MS)
        ensureActive()
        if (currentChannel?.id == channel.id &&
          !isUserPaused &&
          player.playbackState == Player.STATE_BUFFERING
        ) {
          if (!tryScheduleAutoReconnect(channel)) {
            if (onStreamFailure?.invoke(channel) != true) {
              _playbackStatus.value =
                PlaybackStatus.Error(
                  message = "Le flux ne répond plus après plusieurs tentatives.",
                  codeName = "STREAM_STALL_TIMEOUT",
                )
            }
          }
        }
      }
  }

  private fun scheduleSteadyAttemptReset() {
    if (autoReconnectAttempt == 0) return
    val channel = currentChannel ?: return
    if (steadyResetJob?.isActive == true) return

    steadyResetJob =
      scope.launch {
        delay(STEADY_PLAYBACK_RESET_MS)
        ensureActive()
        if (currentChannel?.id == channel.id && _playbackStatus.value is PlaybackStatus.Playing) {
          autoReconnectAttempt = 0
        }
      }
  }

  private inline fun <reified T : Throwable> hasCause(throwable: Throwable?): Boolean {
    var cur = throwable
    var depth = 0
    while (cur != null && depth < 12) {
      if (cur is T) return true
      cur = cur.cause
      depth++
    }
    return false
  }

  private companion object {
    const val MAX_AUTO_RECONNECT_ATTEMPTS = 3
    const val AUTO_RECONNECT_BASE_DELAY_MS = 1_500L
    const val BUFFERING_STALL_TIMEOUT_MS = 12_000L
    const val STEADY_PLAYBACK_RESET_MS = 10_000L
    const val DEFAULT_USER_AGENT =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
  }
}
