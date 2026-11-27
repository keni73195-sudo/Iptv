package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.model.AspectRatioMode
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.PlaybackStatus
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.OrangeLive
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
  isTelevision: Boolean = false,
  dialedChannelInput: String? = null,
  onExpandFromPreview: () -> Unit = {},
) {
  var controlsVisible by remember { mutableStateOf(true) }
  var interactionTick by remember { mutableIntStateOf(0) }
  var isRootSelfFocused by remember { mutableStateOf(false) }
  var focusPlayPausePending by remember { mutableStateOf(false) }

  val rootFocusRequester = remember { FocusRequester() }
  val playPauseFocusRequester = remember { FocusRequester() }
  val errorNextFocusRequester = remember { FocusRequester() }
  val currentTimeText = rememberCurrentTimeText()

  LaunchedEffect(playbackStatus, interactionTick, activeChannel?.id, isMiniPreview, isFullscreen) {
    if (playbackStatus is PlaybackStatus.Playing && !isMiniPreview) {
      controlsVisible = true
      delay(4_500L)
      controlsVisible = false
    } else {
      controlsVisible = true
    }
  }

  LaunchedEffect(isFullscreen) {
    if (isFullscreen) {
      interactionTick++
      delay(80L)
      runCatching { rootFocusRequester.requestFocus() }
    }
  }

  LaunchedEffect(controlsVisible, isFullscreen, playbackStatus) {
    if (isFullscreen && !controlsVisible && playbackStatus is PlaybackStatus.Playing) {
      delay(60L)
      runCatching { rootFocusRequester.requestFocus() }
    }
  }

  LaunchedEffect(focusPlayPausePending, controlsVisible) {
    if (focusPlayPausePending && controlsVisible) {
      delay(50L)
      runCatching { playPauseFocusRequester.requestFocus() }
      focusPlayPausePending = false
    }
  }

  LaunchedEffect(playbackStatus, isFullscreen) {
    if (playbackStatus is PlaybackStatus.Error && isFullscreen) {
      delay(80L)
      runCatching { errorNextFocusRequester.requestFocus() }
    }
  }

  val resizeMode =
    when {
      isMiniPreview -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
      aspectRatioMode == AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
      aspectRatioMode == AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
      else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

  val categoryText = activeChannel?.let { categoryLabel(it.category) }
  val channelSubtitle =
    remember(activeChannel?.id, categoryText, liveResolutionLabel) {
      if (activeChannel == null) {
        "Sélectionnez une chaîne pour démarrer"
      } else {
        val resTag = liveResolutionLabel ?: activeChannel.resolution
        listOfNotNull(
          "Chaîne ${activeChannel.number}",
          categoryText,
          resTag?.takeIf { it.isNotBlank() },
        ).joinToString("  •  ")
      }
    }

  Box(
    modifier =
      modifier
        .background(Color.Black)
        .then(
          if (!isFullscreen && !isMiniPreview && isRootSelfFocused) {
            Modifier.border(2.5.dp, OrangeAccent, RoundedCornerShape(20.dp))
          } else {
            Modifier
          }
        )
        .then(
          if (isMiniPreview) {
            Modifier
          } else {
            Modifier
              .focusRequester(rootFocusRequester)
              .onFocusChanged { isRootSelfFocused = it.isFocused }
              .focusable()
              .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                  val key = keyEvent.key
                  val isOkKey =
                    key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter
                  val isNavKey =
                    isOkKey ||
                      key == Key.DirectionUp ||
                      key == Key.DirectionDown ||
                      key == Key.DirectionLeft ||
                      key == Key.DirectionRight

                  if (isFullscreen) {
                    if (!controlsVisible && playbackStatus is PlaybackStatus.Playing) {
                      when (key) {
                        Key.DirectionUp -> {
                          onPreviousChannel()
                          controlsVisible = true
                          interactionTick++
                          return@onPreviewKeyEvent true
                        }
                        Key.DirectionDown -> {
                          onNextChannel()
                          controlsVisible = true
                          interactionTick++
                          return@onPreviewKeyEvent true
                        }
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter,
                        Key.DirectionLeft,
                        Key.DirectionRight -> {
                          controlsVisible = true
                          interactionTick++
                          focusPlayPausePending = true
                          return@onPreviewKeyEvent true
                        }
                      }
                    } else if (isNavKey) {
                      interactionTick++
                      if (isRootSelfFocused && isOkKey) {
                        if (playbackStatus is PlaybackStatus.Idle) {
                          onTogglePlayPause()
                        } else {
                          focusPlayPausePending = true
                        }
                        return@onPreviewKeyEvent true
                      }
                    }
                  } else {
                    if (isRootSelfFocused && isOkKey) {
                      if (playbackStatus is PlaybackStatus.Idle) {
                        onTogglePlayPause()
                      } else {
                        onToggleFullscreen()
                      }
                      return@onPreviewKeyEvent true
                    }
                    if (isNavKey) {
                      interactionTick++
                    }
                  }
                }
                false
              }
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
              ) {
                if (isFullscreen) {
                  controlsVisible = !controlsVisible
                  if (controlsVisible) interactionTick++
                } else {
                  controlsVisible = !controlsVisible
                  if (controlsVisible) interactionTick++
                }
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

    // Startup or Idle state: prompt user to start channel
    if (playbackStatus is PlaybackStatus.Idle && !isMiniPreview) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.radialGradient(
                colors = listOf(Color(0xFF1E2838), Color(0xFF090D14)),
                radius = 700f,
              )
            ),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(20.dp),
        ) {
          var isPlayFocused by remember { mutableStateOf(false) }
          Surface(
            color = OrangeAccent,
            shape = CircleShape,
            border = if (isPlayFocused) BorderStroke(2.5.dp, Color.White) else null,
            modifier =
              Modifier
                .size(60.dp)
                .clip(CircleShape)
                .onFocusChanged { isPlayFocused = it.isFocused }
                .clickable {
                  interactionTick++
                  onTogglePlayPause()
                }
                .testTag("startup_play_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_pause),
                tint = Color.White,
                modifier = Modifier.size(36.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = activeChannel?.displayTitle ?: "Rifi stream",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Sélectionnez une chaîne pour lancer la lecture",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
          )
        }
      }
    }

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
      Box(modifier = Modifier.fillMaxSize()) {
        // Top controls bar (fades out during continuous playback)
        AnimatedVisibility(
          visible = controlsVisible || playbackStatus !is PlaybackStatus.Playing,
          enter = fadeIn(tween(180)),
          exit = fadeOut(tween(260)),
          modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        ) {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(
                  Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.78f),
                    1f to Color.Transparent,
                  )
                )
                .then(
                  if (isFullscreen) {
                    Modifier.safeDrawingPadding().padding(start = 16.dp, end = 104.dp, top = 10.dp, bottom = 10.dp)
                  } else {
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                  }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (isFullscreen && activeChannel != null) {
                Surface(
                  color = OrangeAccent.copy(alpha = 0.22f),
                  shape = RoundedCornerShape(6.dp),
                  border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.65f)),
                ) {
                  Text(
                    text = "CH ${activeChannel.number}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
              }

              val resTag = liveResolutionLabel ?: activeChannel?.resolution
              if (!resTag.isNullOrBlank()) {
                Surface(
                  color = Color.Black.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(1.dp, Color(0xFF374151)),
                ) {
                  Text(
                    text = resTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              HudActionButton(
                onClick = {
                  interactionTick++
                  onToggleMute()
                },
                onFocused = { interactionTick++ },
                iconRes = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up,
                contentDescription = stringResource(R.string.cd_mute_unmute),
                tint = if (isMuted) AmberFavorite else Color.White,
                modifier = Modifier.testTag("player_mute_button"),
              )

              HudActionButton(
                onClick = {
                  interactionTick++
                  onCycleAspectRatio()
                },
                onFocused = { interactionTick++ },
                iconRes = R.drawable.ic_aspect_ratio,
                contentDescription = stringResource(R.string.cd_aspect_ratio),
                modifier = Modifier.testTag("player_aspect_button"),
              )

              HudActionButton(
                onClick = {
                  interactionTick++
                  onToggleFullscreen()
                },
                onFocused = { interactionTick++ },
                iconRes =
                  if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
                contentDescription =
                  stringResource(
                    if (isFullscreen) R.string.cd_exit_fullscreen else R.string.cd_fullscreen
                  ),
                modifier = Modifier.testTag("player_fullscreen_button"),
              )
            }
          }
        }

        // Fullscreen Always-On Top-Right Live Clock (+ Numeric Channel Dial OSD)
        if (isFullscreen) {
          Column(
            horizontalAlignment = Alignment.End,
            modifier =
              Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
          ) {
            Surface(
              color = Color.Black.copy(alpha = 0.58f),
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.testTag("fullscreen_live_clock"),
            ) {
              Text(
                text = currentTimeText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              )
            }

            if (!dialedChannelInput.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                color = OrangeAccent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("fullscreen_dial_osd"),
              ) {
                Text(
                  text = "CH $dialedChannelInput",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
              }
            }
          }
        }

        // Center Stage: Buffering, Auto-Reconnecting, or Error state
        if (playbackStatus is PlaybackStatus.Buffering || playbackStatus is PlaybackStatus.Reconnecting) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(
                color = OrangeAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(42.dp),
              )
              if (playbackStatus is PlaybackStatus.Reconnecting) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                  color = Color.Black.copy(alpha = 0.72f),
                  shape = RoundedCornerShape(999.dp),
                  border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.65f)),
                  modifier = Modifier.testTag("player_reconnecting_badge"),
                ) {
                  Text(
                    text = "Reconnexion (${playbackStatus.attempt}/${playbackStatus.maxAttempts})…",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                  )
                }
              }
            }
          }
        } else if (playbackStatus is PlaybackStatus.Error) {
          Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              color = Color(0xFF131722),
              shape = RoundedCornerShape(18.dp),
              border = BorderStroke(1.dp, Color(0xFF2E384D)),
              modifier = Modifier.fillMaxWidth(0.88f).padding(16.dp),
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Surface(
                  color = Color(0xFFEF4444).copy(alpha = 0.16f),
                  shape = CircleShape,
                  modifier = Modifier.size(52.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      painter = painterResource(R.drawable.ic_tv_off),
                      contentDescription = null,
                      tint = Color(0xFFFF5252),
                      modifier = Modifier.size(28.dp),
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                  text = stringResource(R.string.error_playback_title),
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                  textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (activeChannel != null) {
                  Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(vertical = 2.dp),
                  ) {
                    Text(
                      text = activeChannel.displayTitle,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = OrangeAccent,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                  text = stringResource(R.string.error_playback_hint),
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFF94A3B8),
                  textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(18.dp))

                var isNextFocused by remember { mutableStateOf(false) }
                var isRetryFocused by remember { mutableStateOf(false) }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Button(
                    onClick = {
                      interactionTick++
                      onNextChannel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                    border = if (isNextFocused) BorderStroke(2.5.dp, Color.White) else null,
                    shape = RoundedCornerShape(999.dp),
                    modifier =
                      Modifier
                        .height(42.dp)
                        .focusRequester(errorNextFocusRequester)
                        .onFocusChanged { isNextFocused = it.isFocused }
                        .testTag("player_error_next_channel_button"),
                  ) {
                    Icon(
                      painter = painterResource(R.drawable.ic_skip_next),
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(R.string.btn_next_channel),
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    )
                  }

                  Surface(
                    color = if (isRetryFocused) Color(0xFF28334A) else Color(0xFF1E2638),
                    shape = RoundedCornerShape(999.dp),
                    border =
                      BorderStroke(
                        width = if (isRetryFocused) 2.dp else 1.dp,
                        color = if (isRetryFocused) OrangeAccent else Color(0xFF334155),
                      ),
                    modifier =
                      Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .onFocusChanged { isRetryFocused = it.isFocused }
                        .clickable {
                          interactionTick++
                          onRetry()
                        }
                        .testTag("player_error_retry_button"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = stringResource(R.string.retry_button),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Bottom Broadcast Overlay (auto-hides in Fullscreen; stays visible in preview card)
        AnimatedVisibility(
          visible = !isFullscreen || controlsVisible || playbackStatus !is PlaybackStatus.Playing,
          enter = fadeIn(tween(180)),
          exit = fadeOut(tween(260)),
          modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
        ) {
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(
                  Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Black.copy(alpha = 0.72f),
                    1f to Color.Black.copy(alpha = 0.95f),
                  )
                )
                .then(
                  if (isFullscreen) {
                    Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)
                  } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                  }
                ),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth(),
            ) {
              LiveBadge(isLive = playbackStatus is PlaybackStatus.Playing)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = activeChannel?.displayTitle ?: "Rifi stream",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = channelSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFD1D5DB),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            // Fullscreen TV-Remote & Touch Transport Controls Bar
            if (isFullscreen) {
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  HudActionButton(
                    onClick = {
                      interactionTick++
                      onPreviousChannel()
                    },
                    onFocused = { interactionTick++ },
                    iconRes = R.drawable.ic_skip_previous,
                    contentDescription = stringResource(R.string.cd_previous_channel),
                    buttonSize = 42.dp,
                    modifier = Modifier.testTag("player_prev_button"),
                  )

                  val isPlayingOrBuffering =
                    playbackStatus is PlaybackStatus.Playing ||
                      playbackStatus is PlaybackStatus.Buffering ||
                      playbackStatus is PlaybackStatus.Reconnecting
                  HudActionButton(
                    onClick = {
                      interactionTick++
                      onTogglePlayPause()
                    },
                    onFocused = { interactionTick++ },
                    iconRes = if (isPlayingOrBuffering) R.drawable.ic_pause else null,
                    imageVector = if (isPlayingOrBuffering) null else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    isHighlighted = true,
                    buttonSize = 46.dp,
                    modifier =
                      Modifier
                        .focusRequester(playPauseFocusRequester)
                        .testTag("player_play_pause_button"),
                  )

                  HudActionButton(
                    onClick = {
                      interactionTick++
                      onNextChannel()
                    },
                    onFocused = { interactionTick++ },
                    iconRes = R.drawable.ic_skip_next,
                    contentDescription = stringResource(R.string.cd_next_channel),
                    buttonSize = 42.dp,
                    modifier = Modifier.testTag("player_next_button"),
                  )

                  HudActionButton(
                    onClick = {
                      interactionTick++
                      onToggleFavorite()
                    },
                    onFocused = { interactionTick++ },
                    iconRes = if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border,
                    contentDescription = stringResource(R.string.cd_favorite_toggle),
                    tint = if (isFavorite) AmberFavorite else Color.White,
                    buttonSize = 42.dp,
                    modifier = Modifier.testTag("player_favorite_button"),
                  )
                }

                if (isTelevision) {
                  Text(
                    text = stringResource(R.string.fullscreen_remote_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
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
private fun HudActionButton(
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
  isHighlighted: Boolean = false,
  tint: Color = Color.White,
  buttonSize: Dp = 38.dp,
  @DrawableRes iconRes: Int? = null,
  imageVector: ImageVector? = null,
) {
  var isFocused by remember { mutableStateOf(false) }

  Surface(
    color =
      when {
        isFocused -> OrangeAccent.copy(alpha = 0.32f)
        isHighlighted -> OrangeAccent.copy(alpha = 0.24f)
        else -> Color(0xFF0F1420).copy(alpha = 0.72f)
      },
    shape = CircleShape,
    border =
      BorderStroke(
        width = if (isFocused) 2.5.dp else 1.dp,
        color =
          when {
            isFocused -> OrangeAccent
            isHighlighted -> OrangeAccent.copy(alpha = 0.75f)
            else -> Color.White.copy(alpha = 0.18f)
          },
      ),
    modifier =
      modifier
        .size(buttonSize)
        .clip(CircleShape)
        .onFocusChanged {
          isFocused = it.isFocused
          if (it.isFocused) onFocused()
        }
        .clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (iconRes != null) {
        Icon(
          painter = painterResource(iconRes),
          contentDescription = contentDescription,
          tint = tint,
          modifier = Modifier.size(buttonSize * 0.52f),
        )
      } else if (imageVector != null) {
        Icon(
          imageVector = imageVector,
          contentDescription = contentDescription,
          tint = tint,
          modifier = Modifier.size(buttonSize * 0.58f),
        )
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
    color = if (isLive) OrangeLive else Color(0xFF26334A),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier.testTag("live_badge_indicator"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(7.dp)
            .alpha(if (isLive) pulseAlpha else 0.7f)
            .clip(CircleShape)
            .background(Color.White)
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = "EN DIRECT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
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
    Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
      Box(
        modifier =
          Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 3.dp)
            .width(30.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.5f))
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(OrangeAccent)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = activeChannel?.displayTitle ?: "Rifi stream",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Surface(
          color = Color(0xFF0F1420).copy(alpha = 0.75f),
          shape = CircleShape,
          modifier =
            Modifier
              .size(28.dp)
              .clip(CircleShape)
              .clickable(onClick = onExpandFromPreview),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              painter = painterResource(R.drawable.ic_open_in_full),
              contentDescription = stringResource(R.string.mini_preview_expand),
              tint = Color.White,
              modifier = Modifier.size(15.dp),
            )
          }
        }
      }
    }

    if (playbackStatus is PlaybackStatus.Buffering || playbackStatus is PlaybackStatus.Reconnecting) {
      CircularProgressIndicator(
        color = OrangeAccent,
        strokeWidth = 2.5.dp,
        modifier = Modifier.size(24.dp).align(Alignment.Center),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        color = OrangeAccent.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, OrangeAccent),
      ) {
        Text(
          text = "DIRECT",
          style = MaterialTheme.typography.labelSmall,
          color = OrangeAccent,
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier.size(28.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
          )
        }
        IconButton(
          onClick = onToggleMute,
          modifier = Modifier.size(28.dp),
        ) {
          Icon(
            painter = painterResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}
