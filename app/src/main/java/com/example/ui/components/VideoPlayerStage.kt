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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
  isMiniPreview: Boolean = false,
  onExpandFromPreview: () -> Unit = {},
) {
  var controlsVisible by remember { mutableStateOf(true) }
  var interactionTick by remember { mutableIntStateOf(0) }

  LaunchedEffect(playbackStatus, interactionTick, activeChannel?.id, isMiniPreview) {
    if (playbackStatus is PlaybackStatus.Playing && !isMiniPreview) {
      controlsVisible = true
      delay(3_500L)
      controlsVisible = false
    } else if (!isMiniPreview) {
      controlsVisible = true
    } else {
      controlsVisible = true
    }
  }

  val resizeMode =
    when {
      isMiniPreview -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
      aspectRatioMode == AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
      aspectRatioMode == AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
      else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

  Box(
    modifier =
      modifier
        .background(Color.Black)
        .then(
          if (isMiniPreview) {
            Modifier
          } else {
            Modifier
              .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                  val isNavKey =
                    keyEvent.key == Key.DirectionCenter ||
                      keyEvent.key == Key.Enter ||
                      keyEvent.key == Key.NumPadEnter ||
                      keyEvent.key == Key.DirectionUp ||
                      keyEvent.key == Key.DirectionDown ||
                      keyEvent.key == Key.DirectionLeft ||
                      keyEvent.key == Key.DirectionRight
                  if (isNavKey) {
                    val wasHidden = !controlsVisible
                    controlsVisible = true
                    interactionTick++
                    if (wasHidden && (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                      return@onPreviewKeyEvent true
                    }
                  }
                }
                false
              }
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
              ) {
                controlsVisible = !controlsVisible
                if (controlsVisible) interactionTick++
              }
          }
        )
  ) {
    AndroidView(
      factory = { context ->
        PlayerView(context).apply {
          useController = false
          keepScreenOn = true
          isFocusable = false
          isFocusableInTouchMode = false
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

    if (isMiniPreview) {
      MiniPreviewOverlay(
        activeChannel = activeChannel,
        playbackStatus = playbackStatus,
        isMuted = isMuted,
        onTogglePlayPause = onTogglePlayPause,
        onRetry = onRetry,
        onToggleMute = onToggleMute,
        onNextChannel = onNextChannel,
        onExpandFromPreview = onExpandFromPreview,
      )
    } else {
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
                  0f to Color.Black.copy(alpha = 0.76f),
                  0.35f to Color.Transparent,
                  0.65f to Color.Transparent,
                  1f to Color.Black.copy(alpha = 0.86f),
                )
              )
        ) {
          val overlayPaddingModifier =
            if (isFullscreen) {
              Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 10.dp)
            } else {
              Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
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
                    border = BorderStroke(1.dp, CyanBroadcast.copy(alpha = 0.45f)),
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
                    modifier =
                      Modifier
                        .minimumInteractiveComponentSize()
                        .then(tvFocusHighlight())
                        .testTag("player_favorite_button"),
                  ) {
                    Icon(
                      painter = painterResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border),
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
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .then(tvFocusHighlight())
                      .testTag("player_aspect_button"),
                ) {
                  Icon(
                    painter = painterResource(R.drawable.ic_aspect_ratio),
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
                      modifier = Modifier.size(38.dp),
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
                    color = ObsidianBg.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonLive.copy(alpha = 0.55f)),
                    modifier = Modifier.padding(horizontal = 16.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                      Icon(
                        painter = painterResource(R.drawable.ic_error_outline),
                        contentDescription = null,
                        tint = CrimsonLive,
                        modifier = Modifier.size(26.dp),
                      )
                      Spacer(modifier = Modifier.height(4.dp))
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
                  var isCenterFocused by remember { mutableStateOf(false) }
                  Surface(
                    color = ObsidianBg.copy(alpha = if (isCenterFocused) 0.88f else 0.68f),
                    shape = CircleShape,
                    border =
                      BorderStroke(
                        width = if (isCenterFocused) 2.dp else 1.dp,
                        color = if (isCenterFocused) CyanBroadcast else BorderSlate,
                      ),
                    modifier =
                      Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .onFocusChanged { isCenterFocused = it.isFocused }
                        .clickable {
                          interactionTick++
                          onTogglePlayPause()
                        }
                        .testTag("player_center_play_pause_button"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      if (isPlaying) {
                        Icon(
                          painter = painterResource(R.drawable.ic_pause),
                          contentDescription = stringResource(R.string.cd_play_pause),
                          tint = InkPrimary,
                          modifier = Modifier.size(28.dp),
                        )
                      } else {
                        Icon(
                          imageVector = Icons.Rounded.PlayArrow,
                          contentDescription = stringResource(R.string.cd_play_pause),
                          tint = InkPrimary,
                          modifier = Modifier.size(28.dp),
                        )
                      }
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
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .then(tvFocusHighlight())
                      .testTag("player_prev_button"),
                ) {
                  Icon(
                    painter = painterResource(R.drawable.ic_skip_previous),
                    contentDescription = stringResource(R.string.cd_previous_channel),
                    tint = InkPrimary,
                  )
                }

                IconButton(
                  onClick = {
                    interactionTick++
                    onTogglePlayPause()
                  },
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .then(tvFocusHighlight())
                      .testTag("player_play_pause_button"),
                ) {
                  if (playbackStatus is PlaybackStatus.Playing) {
                    Icon(
                      painter = painterResource(R.drawable.ic_pause),
                      contentDescription = stringResource(R.string.cd_play_pause),
                      tint = InkPrimary,
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Rounded.PlayArrow,
                      contentDescription = stringResource(R.string.cd_play_pause),
                      tint = InkPrimary,
                    )
                  }
                }

                IconButton(
                  onClick = {
                    interactionTick++
                    onNextChannel()
                  },
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .then(tvFocusHighlight())
                      .testTag("player_next_button"),
                ) {
                  Icon(
                    painter = painterResource(R.drawable.ic_skip_next),
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
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .then(tvFocusHighlight())
                      .testTag("player_mute_button"),
                ) {
                  Icon(
                    painter =
                      painterResource(
                        if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
                      ),
                    contentDescription = stringResource(R.string.cd_mute_unmute),
                    tint = if (isMuted) AmberFavorite else InkPrimary,
                  )
                }

                IconButton(
                  onClick = {
                    interactionTick++
                    onToggleFullscreen()
                  },
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .then(tvFocusHighlight())
                      .testTag("player_fullscreen_button"),
                ) {
                  Icon(
                    painter =
                      painterResource(
                        if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
                      ),
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
}

@Composable
private fun MiniPreviewOverlay(
  activeChannel: Channel?,
  playbackStatus: PlaybackStatus,
  isMuted: Boolean,
  onTogglePlayPause: () -> Unit,
  onRetry: () -> Unit,
  onToggleMute: () -> Unit,
  onNextChannel: () -> Unit,
  onExpandFromPreview: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.72f),
            0.45f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.88f),
          )
        )
        .padding(6.dp)
  ) {
    // Top drag handle + bar inside floating preview
    Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
      Box(
        modifier =
          Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 3.dp)
            .width(30.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(InkPrimary.copy(alpha = 0.48f))
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (playbackStatus is PlaybackStatus.Playing) CrimsonLive else CyanBroadcast)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = activeChannel?.displayTitle ?: stringResource(R.string.preview_badge),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Surface(
          color = ObsidianBg.copy(alpha = 0.75f),
          shape = CircleShape,
          modifier =
            Modifier
              .size(28.dp)
              .clip(CircleShape)
              .clickable(onClick = onExpandFromPreview)
              .testTag("mini_preview_expand_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              painter = painterResource(R.drawable.ic_open_in_full),
              contentDescription = stringResource(R.string.mini_preview_expand),
              tint = InkPrimary,
              modifier = Modifier.size(15.dp),
            )
          }
        }
      }
    }

    // Center mini status indicator (Buffering / Error)
    if (playbackStatus is PlaybackStatus.Buffering) {
      CircularProgressIndicator(
        color = CyanBroadcast,
        strokeWidth = 2.5.dp,
        modifier = Modifier.size(24.dp).align(Alignment.Center),
      )
    } else if (playbackStatus is PlaybackStatus.Error) {
      Surface(
        color = ObsidianBg.copy(alpha = 0.85f),
        shape = CircleShape,
        border = BorderStroke(1.dp, CrimsonLive.copy(alpha = 0.7f)),
        modifier =
          Modifier
            .size(34.dp)
            .clip(CircleShape)
            .align(Alignment.Center)
            .clickable(onClick = onRetry),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.retry_button),
            tint = CrimsonLive,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    // Bottom quick controls inside floating preview
    Row(
      modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        color = CyanBroadcast.copy(alpha = 0.16f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, CyanBroadcast.copy(alpha = 0.45f)),
      ) {
        Text(
          text = stringResource(R.string.preview_badge),
          style = MaterialTheme.typography.labelSmall,
          color = CyanBroadcast,
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MiniPreviewActionChip(
          onClick = onTogglePlayPause,
          testTag = "mini_preview_play_pause",
        ) {
          if (playbackStatus is PlaybackStatus.Playing) {
            Icon(
              painter = painterResource(R.drawable.ic_pause),
              contentDescription = stringResource(R.string.cd_play_pause),
              tint = InkPrimary,
              modifier = Modifier.size(16.dp),
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = stringResource(R.string.cd_play_pause),
              tint = InkPrimary,
              modifier = Modifier.size(16.dp),
            )
          }
        }
        MiniPreviewActionChip(
          onClick = onToggleMute,
          testTag = "mini_preview_mute",
        ) {
          Icon(
            painter =
              painterResource(
                if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
              ),
            contentDescription = stringResource(R.string.cd_mute_unmute),
            tint = if (isMuted) AmberFavorite else InkPrimary,
            modifier = Modifier.size(16.dp),
          )
        }
        MiniPreviewActionChip(
          onClick = onNextChannel,
          testTag = "mini_preview_next",
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_skip_next),
            contentDescription = stringResource(R.string.cd_next_channel),
            tint = InkPrimary,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MiniPreviewActionChip(
  onClick: () -> Unit,
  testTag: String,
  content: @Composable () -> Unit,
) {
  Surface(
    color = ObsidianBg.copy(alpha = 0.78f),
    shape = CircleShape,
    border = BorderStroke(0.8.dp, BorderSlate),
    modifier =
      Modifier
        .size(28.dp)
        .clip(CircleShape)
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.Center) { content() }
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
    color = if (isLive) CrimsonLive.copy(alpha = 0.22f) else BorderSlate.copy(alpha = 0.6f),
    shape = RoundedCornerShape(6.dp),
    border =
      BorderStroke(
        1.dp,
        if (isLive) CrimsonLive.copy(alpha = 0.75f) else BorderSlate,
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

@Composable
private fun tvFocusHighlight(shape: RoundedCornerShape = CircleShape): Modifier {
  var isFocused by remember { mutableStateOf(false) }
  return Modifier
    .onFocusChanged { isFocused = it.isFocused }
    .then(
      if (isFocused) {
        Modifier.border(2.dp, CyanBroadcast, shape)
      } else {
        Modifier
      }
    )
}
