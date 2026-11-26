package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.model.AspectRatioMode
import com.example.model.Channel
import com.example.model.PlaybackStatus
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanBroadcast
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerStage(
  player: Player,
  activeChannel: Channel?,
  playbackStatus: PlaybackStatus,
  isMuted: Boolean,
  isFavorite: Boolean,
  liveResolutionLabel: String?,
  aspectRatioMode: AspectRatioMode,
  isFullscreen: Boolean,
  onTogglePlayPause: () -> Unit,
  onRetry: () -> Unit,
  onToggleMute: () -> Unit,
  onPreviousChannel: () -> Unit,
  onNextChannel: () -> Unit,
  onToggleFavorite: () -> Unit,
  onCycleAspectRatio: () -> Unit,
  onToggleFullscreen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var controlsVisible by remember { mutableStateOf(true) }
  var interactionTick by remember { mutableIntStateOf(0) }

  LaunchedEffect(playbackStatus, interactionTick, activeChannel?.id) {
    if (playbackStatus is PlaybackStatus.Playing) {
      controlsVisible = true
      delay(3_500L)
      controlsVisible = false
    } else {
      controlsVisible = true
    }
  }

  val resizeMode =
    when (aspectRatioMode) {
      AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
      AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
      AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

  Box(
    modifier =
      modifier
        .background(Color.Black)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          controlsVisible = !controlsVisible
          if (controlsVisible) interactionTick++
        }
  ) {
    AndroidView(
      factory = { context ->
        PlayerView(context).apply {
          useController = false
          keepScreenOn = true
          setShutterBackgroundColor(android.graphics.Color.BLACK)
          layoutParams =
            FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          this.player = player
        }
      },
      update = { playerView ->
        if (playerView.player !== player) {
          playerView.player = player
        }
        if (playerView.resizeMode != resizeMode) {
          playerView.resizeMode = resizeMode
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Subtle top and bottom cinema scrims when controls are visible
    AnimatedVisibility(
      visible = controlsVisible || playbackStatus !is PlaybackStatus.Playing,
      enter = fadeIn(tween(180)),
      exit = fadeOut(tween(260)),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.72f),
                0.35f to Color.Transparent,
                0.65f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.82f),
              )
            )
      ) {
        val overlayPaddingModifier =
          if (isFullscreen) {
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 10.dp)
          } else {
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)
          }

        Column(
          modifier = overlayPaddingModifier,
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Top Overlay: Live Indicator + Active Channel + Quick Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              LiveBadge(isLive = playbackStatus is PlaybackStatus.Playing)
              Spacer(modifier = Modifier.width(8.dp))
              if (activeChannel != null) {
                Text(
                  text = activeChannel.displayTitle,
                  style = MaterialTheme.typography.titleMedium,
                  color = InkPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
              }
              val badgeText = liveResolutionLabel ?: activeChannel?.resolution
              if (!badgeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  color = CyanBroadcast.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(6.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, CyanBroadcast.copy(alpha = 0.45f)),
                ) {
                  Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanBroadcast,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (activeChannel != null) {
                IconButton(
                  onClick = {
                    interactionTick++
                    onToggleFavorite()
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("player_favorite_button"),
                ) {
                  Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = stringResource(R.string.cd_favorite_toggle),
                    tint = if (isFavorite) AmberFavorite else InkPrimary,
                  )
                }
              }

              IconButton(
                onClick = {
                  interactionTick++
                  onCycleAspectRatio()
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("player_aspect_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AspectRatio,
                  contentDescription = stringResource(R.string.cd_aspect_ratio),
                  tint = InkPrimary,
                )
              }
            }
          }

          // Center Stage: Buffering / Error / Play-Pause
          Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
          ) {
            when (playbackStatus) {
              is PlaybackStatus.Buffering -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  CircularProgressIndicator(
                    color = CyanBroadcast,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = stringResource(R.string.buffering_status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkPrimary,
                  )
                }
              }
              is PlaybackStatus.Error -> {
                Surface(
                  color = ObsidianBg.copy(alpha = 0.90f),
                  shape = RoundedCornerShape(14.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonLive.copy(alpha = 0.5f)),
                  modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                  Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.ErrorOutline,
                      contentDescription = null,
                      tint = CrimsonLive,
                      modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = stringResource(R.string.error_playback_title),
                      style = MaterialTheme.typography.titleMedium,
                      color = InkPrimary,
                    )
                    Text(
                      text = playbackStatus.message,
                      style = MaterialTheme.typography.bodyMedium,
                      color = InkSecondary,
                      textAlign = TextAlign.Center,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                      onClick = {
                        interactionTick++
                        onRetry()
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = CyanBroadcast, contentColor = ObsidianBg),
                      modifier = Modifier.testTag("player_retry_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(text = stringResource(R.string.retry_button))
                    }
                  }
                }
              }
              else -> {
                val isPlaying = playbackStatus is PlaybackStatus.Playing
                Surface(
                  color = ObsidianBg.copy(alpha = 0.65f),
                  shape = CircleShape,
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                  modifier =
                    Modifier
                      .size(56.dp)
                      .clip(CircleShape)
                      .clickable {
                        interactionTick++
                        onTogglePlayPause()
                      }
                      .testTag("player_center_play_pause_button"),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                      contentDescription = stringResource(R.string.cd_play_pause),
                      tint = InkPrimary,
                      modifier = Modifier.size(30.dp),
                    )
                  }
                }
              }
            }
          }

          // Bottom Transport Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  interactionTick++
                  onPreviousChannel()
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("player_prev_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.SkipPrevious,
                  contentDescription = stringResource(R.string.cd_previous_channel),
                  tint = InkPrimary,
                )
              }

              IconButton(
                onClick = {
                  interactionTick++
                  onTogglePlayPause()
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("player_play_pause_button"),
              ) {
                Icon(
                  imageVector =
                    if (playbackStatus is PlaybackStatus.Playing) {
                      Icons.Rounded.Pause
                    } else {
                      Icons.Rounded.PlayArrow
                    },
                  contentDescription = stringResource(R.string.cd_play_pause),
                  tint = InkPrimary,
                )
              }

              IconButton(
                onClick = {
                  interactionTick++
                  onNextChannel()
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("player_next_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.SkipNext,
                  contentDescription = stringResource(R.string.cd_next_channel),
                  tint = InkPrimary,
                )
              }

              Spacer(modifier = Modifier.width(4.dp))

              val modeLabel =
                when (aspectRatioMode) {
                  AspectRatioMode.FIT -> "FIT"
                  AspectRatioMode.FILL -> "FILL"
                  AspectRatioMode.ZOOM -> "ZOOM"
                }
              Text(
                text = modeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary,
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  interactionTick++
                  onToggleMute()
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("player_mute_button"),
              ) {
                Icon(
                  imageVector =
                    if (isMuted) {
                      Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                      Icons.AutoMirrored.Rounded.VolumeUp
                    },
                  contentDescription = stringResource(R.string.cd_mute_unmute),
                  tint = if (isMuted) AmberFavorite else InkPrimary,
                )
              }

              IconButton(
                onClick = {
                  interactionTick++
                  onToggleFullscreen()
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("player_fullscreen_button"),
              ) {
                Icon(
                  imageVector =
                    if (isFullscreen) {
                      Icons.Rounded.FullscreenExit
                    } else {
                      Icons.Rounded.Fullscreen
                    },
                  contentDescription =
                    if (isFullscreen) {
                      stringResource(R.string.cd_exit_fullscreen)
                    } else {
                      stringResource(R.string.cd_fullscreen)
                    },
                  tint = InkPrimary,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LiveBadge(isLive: Boolean) {
  val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = 0.25f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(900),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "live_dot_alpha",
    )

  Surface(
    color = if (isLive) CrimsonLive.copy(alpha = 0.20f) else BorderSlate.copy(alpha = 0.6f),
    shape = RoundedCornerShape(6.dp),
    border =
      androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isLive) CrimsonLive.copy(alpha = 0.7f) else BorderSlate,
      ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(7.dp)
            .alpha(if (isLive) pulseAlpha else 0.7f)
            .clip(CircleShape)
            .background(if (isLive) CrimsonLive else InkSecondary)
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = stringResource(R.string.live_badge),
        style = MaterialTheme.typography.labelSmall,
        color = InkPrimary,
      )
    }
  }
}
