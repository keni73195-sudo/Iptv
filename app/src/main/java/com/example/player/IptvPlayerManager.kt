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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.model.Channel
import com.example.model.PlaybackStatus
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
class IptvPlayerManager(context: Context) {

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

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

  private val playerListener =
    object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        updateStatusFromPlayer()
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateStatusFromPlayer()
      }

      override fun onPlayerError(error: PlaybackException) {
        val codeName = error.errorCodeName
        val detail =
          when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
              "Impossible de joindre le serveur de diffusion."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
              "Accès refusé ou flux temporairement indisponible ($codeName)."
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
    currentChannel = channel
    _videoResolutionLabel.value = channel.resolution
    _playbackStatus.value = PlaybackStatus.Buffering

    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
    val userAgent = channel.userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT
    dataSourceFactory.setUserAgent(userAgent)

    val headers = buildMap {
      put("User-Agent", userAgent)
      channel.referrer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
    }
    dataSourceFactory.setDefaultRequestProperties(headers)

    val mediaItemBuilder = MediaItem.Builder().setUri(channel.streamUrl)
    val isHls = channel.streamUrl.contains(".m3u8", ignoreCase = true)
    if (isHls) {
      mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
    }
    val mediaItem = mediaItemBuilder.build()

    val mediaSource =
      if (isHls) {
        HlsMediaSource.Factory(dataSourceFactory)
          .setAllowChunklessPreparation(true)
          .createMediaSource(mediaItem)
      } else {
        DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
      }

    player.stop()
    player.setMediaSource(mediaSource)
    player.prepare()
    player.playWhenReady = true
  }

  fun retryCurrentChannel() {
    currentChannel?.let { playChannel(it) }
  }

  fun togglePlayPause() {
    if (player.playbackState == Player.STATE_IDLE) {
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
    player.removeListener(playerListener)
    player.release()
  }

  private fun updateStatusFromPlayer() {
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
