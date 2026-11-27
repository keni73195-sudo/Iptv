package com.example.ui.screens

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import com.example.R
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.PlaybackStatus
import com.example.ui.IptvUiState
import com.example.ui.components.ActiveChannelTelemetryStrip
import com.example.ui.components.BroadcastTopHeaderAndSearchBar
import com.example.ui.components.ChannelRowCard
import com.example.ui.components.EmptyChannelsState
import com.example.ui.components.ImportPlaylistDialog
import com.example.ui.components.VideoPlayerStage
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CyanBroadcast
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.ObsidianBg
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun IptvScreen(
  uiState: IptvUiState,
  player: Player,
  onSelectChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onTogglePlayPause: () -> Unit,
  onRetryPlayback: () -> Unit,
  onToggleMute: () -> Unit,
  onPreviousChannel: () -> Unit,
  onNextChannel: () -> Unit,
  onCycleAspectRatio: () -> Unit,
  onToggleFullscreen: () -> Unit,
  onExitFullscreen: () -> Unit,
  onCategorySelect: (ChannelCategory) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onOpenImportDialog: () -> Unit,
  onCloseImportDialog: () -> Unit,
  onSyncAllSources: () -> Unit,
  onImportFromUrl: (String, Boolean) -> Unit,
  onImportFromText: (String, Boolean) -> Unit,
  onResetDefaultPlaylist: () -> Unit,
  onClearFeedbackBanner: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val portraitListState = rememberLazyListState()
  val wideListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current
  val isTelevision =
    remember(context) {
      val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
      uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

  LaunchedEffect(uiState.feedbackBanner) {
    val msg = uiState.feedbackBanner
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearFeedbackBanner()
    }
  }

  // Always reset scroll to top when user actively filters or types a new search query
  LaunchedEffect(uiState.selectedCategory, uiState.searchQuery) {
    portraitListState.scrollToItem(0)
    wideListState.scrollToItem(0)
  }

  val channelCounts =
    remember(uiState.allChannels, uiState.favoriteIds) {
      val grouped = uiState.allChannels.groupingBy { it.category }.eachCount()
      ChannelCategory.entries.associateWith { category ->
        when (category) {
          ChannelCategory.ALL -> uiState.allChannels.size
          ChannelCategory.FAVORITES -> uiState.allChannels.count { it.id in uiState.favoriteIds }
          else -> grouped[category] ?: 0
        }
      }
    }

  val activeIsFavorite = uiState.activeChannel?.id?.let { it in uiState.favoriteIds } == true

  if (uiState.isFullscreen) {
    BackHandler { onExitFullscreen() }
    Box(modifier = modifier.fillMaxSize().background(ObsidianBg)) {
      VideoPlayerStage(
        player = player,
        activeChannel = uiState.activeChannel,
        playbackStatus = uiState.playbackStatus,
        isMuted = uiState.isMuted,
        isFavorite = activeIsFavorite,
        liveResolutionLabel = uiState.liveResolutionLabel,
        aspectRatioMode = uiState.aspectRatioMode,
        isFullscreen = true,
        onTogglePlayPause = onTogglePlayPause,
        onRetry = onRetryPlayback,
        onToggleMute = onToggleMute,
        onPreviousChannel = onPreviousChannel,
        onNextChannel = onNextChannel,
        onToggleFavorite = { uiState.activeChannel?.let { onToggleFavorite(it.id) } },
        onCycleAspectRatio = onCycleAspectRatio,
        onToggleFullscreen = onToggleFullscreen,
        modifier = Modifier.fillMaxSize(),
      )
    }
  } else {
    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0f to Color(0xFF0E1726),
                0.28f to ObsidianBg,
                1f to ObsidianBg,
              )
            )
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(
              if (isTelevision) {
                Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
              } else {
                Modifier
              }
            )
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Persistent Top Search Bar + Brand + Category Chips at the very top
          BroadcastTopHeaderAndSearchBar(
            searchQuery = uiState.searchQuery,
            selectedCategory = uiState.selectedCategory,
            channelCounts = channelCounts,
            filteredCount = uiState.filteredChannels.size,
            totalCount = uiState.allChannels.size,
            isSyncingSources = uiState.isSyncingSources,
            onSearchQueryChange = onSearchQueryChange,
            onCategorySelect = onCategorySelect,
            onOpenImport = onOpenImportDialog,
          )

          BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideLayout = maxWidth >= 600.dp || isTelevision

            if (isWideLayout) {
              // Tablet / Landscape Broadcast Control Room: live preview always visible alongside LazyColumn
              Row(modifier = Modifier.fillMaxSize()) {
                Column(
                  modifier =
                    Modifier
                      .weight(0.55f)
                      .fillMaxHeight()
                      .windowInsetsPadding(WindowInsets.navigationBars)
                      .padding(14.dp)
                ) {
                  VideoPlayerStage(
                    player = player,
                    activeChannel = uiState.activeChannel,
                    playbackStatus = uiState.playbackStatus,
                    isMuted = uiState.isMuted,
                    isFavorite = activeIsFavorite,
                    liveResolutionLabel = uiState.liveResolutionLabel,
                    aspectRatioMode = uiState.aspectRatioMode,
                    isFullscreen = false,
                    onTogglePlayPause = onTogglePlayPause,
                    onRetry = onRetryPlayback,
                    onToggleMute = onToggleMute,
                    onPreviousChannel = onPreviousChannel,
                    onNextChannel = onNextChannel,
                    onToggleFavorite = { uiState.activeChannel?.let { onToggleFavorite(it.id) } },
                    onCycleAspectRatio = onCycleAspectRatio,
                    onToggleFullscreen = onToggleFullscreen,
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp)),
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  ActiveChannelTelemetryStrip(
                    activeChannel = uiState.activeChannel,
                    totalChannelCount = uiState.allChannels.size,
                    playbackStatus = uiState.playbackStatus,
                    isSyncingSources = uiState.isSyncingSources,
                    onRetry = onRetryPlayback,
                  )
                }

                ChannelListLazyColumn(
                  uiState = uiState,
                  listState = wideListState,
                  includeHeroDock = false,
                  heroSpacerHeight = 0.dp,
                  onSelectChannel = onSelectChannel,
                  onToggleFavorite = onToggleFavorite,
                  onRetryPlayback = onRetryPlayback,
                  onResetFilters = {
                    onSearchQueryChange("")
                    onCategorySelect(ChannelCategory.ALL)
                  },
                  modifier =
                    Modifier
                      .weight(0.45f)
                      .fillMaxHeight()
                      .windowInsetsPadding(WindowInsets.navigationBars),
                )
              }
            } else {
              // Handheld Portrait: Full-scroll LazyColumn + Hero Player that morphs smoothly into a freely draggable floating Mini-Preview while scrolling
              val density = LocalDensity.current
              val heroWidth = maxWidth - 28.dp
              val heroHeight = minOf(heroWidth * 9f / 16f, 224.dp)
              val miniWidth = 204.dp
              val miniHeight = 116.dp
              val miniTriggerPx = with(density) { 28.dp.roundToPx() }

              val containerWidthPx = with(density) { maxWidth.toPx() }
              val containerHeightPx = with(density) { maxHeight.toPx() }
              val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
              val heroWidthPx = with(density) { heroWidth.toPx() }
              val miniWidthPx = with(density) { miniWidth.toPx() }
              val miniHeightPx = with(density) { miniHeight.toPx() }
              val marginPx = with(density) { 12.dp.toPx() }
              val topDockMarginPx = with(density) { 10.dp.toPx() }

              val maxDragX = (containerWidthPx - miniWidthPx - marginPx).coerceAtLeast(marginPx)
              val maxDragY =
                (containerHeightPx - navBarBottomPx - miniHeightPx - marginPx).coerceAtLeast(marginPx)

              val isMiniPreview by remember(miniTriggerPx) {
                derivedStateOf {
                  portraitListState.firstVisibleItemIndex > 0 ||
                    portraitListState.firstVisibleItemScrollOffset > miniTriggerPx
                }
              }

              var customMiniOffset by remember { mutableStateOf<Offset?>(null) }
              var isDraggingMini by remember { mutableStateOf(false) }

              val heroX = ((containerWidthPx - heroWidthPx) / 2f).coerceAtLeast(0f)
              val heroY = topDockMarginPx
              val miniTargetX = (customMiniOffset?.x ?: maxDragX).coerceIn(marginPx, maxDragX)
              val miniTargetY = (customMiniOffset?.y ?: maxDragY).coerceIn(marginPx, maxDragY)

              val latestMiniX by rememberUpdatedState(miniTargetX)
              val latestMiniY by rememberUpdatedState(miniTargetY)
              val latestMargin by rememberUpdatedState(marginPx)
              val latestMaxX by rememberUpdatedState(maxDragX)
              val latestMaxY by rememberUpdatedState(maxDragY)

              val playerX by
                animateFloatAsState(
                  targetValue = if (isMiniPreview) miniTargetX else heroX,
                  animationSpec =
                    if (isDraggingMini) snap() else spring(stiffness = Spring.StiffnessMediumLow),
                  label = "player_x",
                )
              val playerY by
                animateFloatAsState(
                  targetValue = if (isMiniPreview) miniTargetY else heroY,
                  animationSpec =
                    if (isDraggingMini) snap() else spring(stiffness = Spring.StiffnessMediumLow),
                  label = "player_y",
                )

              val playerWidth by
                animateDpAsState(
                  targetValue = if (isMiniPreview) miniWidth else heroWidth,
                  animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                  label = "player_width",
                )
              val playerHeight by
                animateDpAsState(
                  targetValue = if (isMiniPreview) miniHeight else heroHeight,
                  animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                  label = "player_height",
                )
              val playerCorner by
                animateDpAsState(
                  targetValue = if (isMiniPreview) 14.dp else 16.dp,
                  animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                  label = "player_corner",
                )

              Box(modifier = Modifier.fillMaxSize()) {
                ChannelListLazyColumn(
                  uiState = uiState,
                  listState = portraitListState,
                  includeHeroDock = true,
                  heroSpacerHeight = heroHeight,
                  onSelectChannel = onSelectChannel,
                  onToggleFavorite = onToggleFavorite,
                  onRetryPlayback = onRetryPlayback,
                  onResetFilters = {
                    onSearchQueryChange("")
                    onCategorySelect(ChannelCategory.ALL)
                  },
                  modifier =
                    Modifier
                      .fillMaxSize()
                      .windowInsetsPadding(WindowInsets.navigationBars),
                )

                // Quick Jump-To-Top / Active Channel pill when scrolled into Mini-Preview mode
                androidx.compose.animation.AnimatedVisibility(
                  visible = isMiniPreview,
                  enter = fadeIn(tween(180)),
                  exit = fadeOut(tween(180)),
                  modifier =
                    Modifier
                      .align(Alignment.BottomStart)
                      .windowInsetsPadding(WindowInsets.navigationBars)
                      .padding(start = 14.dp, bottom = 14.dp)
                      .zIndex(2f),
                ) {
                  Surface(
                    color = ElevatedCard.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, CyanBroadcast.copy(alpha = 0.55f)),
                    modifier =
                      Modifier
                        .shadow(8.dp, RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                          coroutineScope.launch { portraitListState.animateScrollToItem(0) }
                        }
                        .testTag("jump_to_active_button"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        painter = painterResource(R.drawable.ic_vertical_align_top),
                        contentDescription = stringResource(R.string.mini_preview_expand),
                        tint = CyanBroadcast,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = uiState.activeChannel?.displayTitle ?: stringResource(R.string.jump_to_live_channel),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(106.dp),
                      )
                    }
                  }
                }

                // Single persistent VideoPlayerStage morphing between Hero Dock and freely draggable Floating Live Preview
                Surface(
                  color = Color.Black,
                  shape = RoundedCornerShape(playerCorner),
                  border =
                    BorderStroke(
                      width = if (isDraggingMini) 2.dp else if (isMiniPreview) 1.5.dp else 1.dp,
                      color =
                        when {
                          isDraggingMini -> CyanBroadcast
                          isMiniPreview -> CyanBroadcast.copy(alpha = 0.78f)
                          else -> BorderSlate
                        },
                    ),
                  modifier =
                    Modifier
                      .offset { IntOffset(playerX.roundToInt(), playerY.roundToInt()) }
                      .zIndex(3f)
                      .width(playerWidth)
                      .height(playerHeight)
                      .shadow(
                        elevation = if (isDraggingMini) 22.dp else if (isMiniPreview) 16.dp else 6.dp,
                        shape = RoundedCornerShape(playerCorner),
                      )
                      .pointerInput(isMiniPreview) {
                        if (isMiniPreview) {
                          awaitEachGesture {
                            val down =
                              awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                              )
                            val touchSlop = viewConfiguration.touchSlop
                            var totalDrag = Offset.Zero
                            var isDrag = false
                            var unconsumedTap = false

                            try {
                              while (true) {
                                val initialEvent = awaitPointerEvent(PointerEventPass.Initial)
                                val change =
                                  initialEvent.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                  if (isDrag) {
                                    change.consume()
                                  } else {
                                    val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                                    val finalChange =
                                      finalEvent.changes.firstOrNull { it.id == down.id }
                                    if (finalChange != null && !finalChange.isConsumed) {
                                      unconsumedTap = true
                                    }
                                  }
                                  break
                                }

                                val delta = change.positionChange()
                                if (!isDrag) {
                                  totalDrag += delta
                                  if (totalDrag.getDistance() > touchSlop) {
                                    isDrag = true
                                    isDraggingMini = true
                                    val startOffset =
                                      customMiniOffset ?: Offset(latestMiniX, latestMiniY)
                                    change.consume()
                                    customMiniOffset =
                                      Offset(
                                        x = (startOffset.x + totalDrag.x).coerceIn(latestMargin, latestMaxX),
                                        y = (startOffset.y + totalDrag.y).coerceIn(latestMargin, latestMaxY),
                                      )
                                  }
                                } else {
                                  change.consume()
                                  val current = customMiniOffset ?: Offset(latestMiniX, latestMiniY)
                                  customMiniOffset =
                                    Offset(
                                      x = (current.x + delta.x).coerceIn(latestMargin, latestMaxX),
                                      y = (current.y + delta.y).coerceIn(latestMargin, latestMaxY),
                                    )
                                }
                              }
                            } finally {
                              isDraggingMini = false
                            }

                            if (unconsumedTap) {
                              coroutineScope.launch { portraitListState.animateScrollToItem(0) }
                            }
                          }
                        }
                      }
                      .testTag("floating_video_player_stage"),
                ) {
                  VideoPlayerStage(
                    player = player,
                    activeChannel = uiState.activeChannel,
                    playbackStatus = uiState.playbackStatus,
                    isMuted = uiState.isMuted,
                    isFavorite = activeIsFavorite,
                    liveResolutionLabel = uiState.liveResolutionLabel,
                    aspectRatioMode = uiState.aspectRatioMode,
                    isFullscreen = false,
                    isMiniPreview = isMiniPreview,
                    onExpandFromPreview = {
                      coroutineScope.launch { portraitListState.animateScrollToItem(0) }
                    },
                    onTogglePlayPause = onTogglePlayPause,
                    onRetry = onRetryPlayback,
                    onToggleMute = onToggleMute,
                    onPreviousChannel = onPreviousChannel,
                    onNextChannel = onNextChannel,
                    onToggleFavorite = { uiState.activeChannel?.let { onToggleFavorite(it.id) } },
                    onCycleAspectRatio = onCycleAspectRatio,
                    onToggleFullscreen = onToggleFullscreen,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (uiState.isImportDialogOpen) {
    ImportPlaylistDialog(
      isImporting = uiState.isImporting,
      isSyncingSources = uiState.isSyncingSources,
      importError = uiState.importError,
      onDismiss = onCloseImportDialog,
      onSyncAllSources = onSyncAllSources,
      onImportFromUrl = onImportFromUrl,
      onImportFromText = onImportFromText,
      onResetDefault = onResetDefaultPlaylist,
    )
  }
}

@Composable
private fun ChannelListLazyColumn(
  uiState: IptvUiState,
  listState: LazyListState,
  includeHeroDock: Boolean,
  heroSpacerHeight: androidx.compose.ui.unit.Dp,
  onSelectChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onRetryPlayback: () -> Unit,
  onResetFilters: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    state = listState,
    contentPadding =
      PaddingValues(
        start = 14.dp,
        end = 14.dp,
        top = 10.dp,
        bottom = if (includeHeroDock) 136.dp else 20.dp,
      ),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.testTag("channel_list_lazy_column"),
  ) {
    if (includeHeroDock) {
      item(key = "hero_dock_spacer") {
        Column(modifier = Modifier.fillMaxWidth()) {
          Spacer(modifier = Modifier.height(heroSpacerHeight + 10.dp))
          ActiveChannelTelemetryStrip(
            activeChannel = uiState.activeChannel,
            totalChannelCount = uiState.allChannels.size,
            playbackStatus = uiState.playbackStatus,
            isSyncingSources = uiState.isSyncingSources,
            onRetry = onRetryPlayback,
          )
        }
      }
    }

    if (uiState.filteredChannels.isEmpty()) {
      item(key = "empty_state") {
        EmptyChannelsState(
          isFavoritesFilter = uiState.selectedCategory == ChannelCategory.FAVORITES,
          onResetFilters =
            if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != ChannelCategory.ALL) {
              onResetFilters
            } else {
              null
            },
        )
      }
    } else {
      items(
        items = uiState.filteredChannels,
        key = { "${it.number}_${it.id}" },
      ) { channel ->
        val isSelected = uiState.activeChannel?.id == channel.id
        ChannelRowCard(
          channel = channel,
          isSelected = isSelected,
          isPlaying = isSelected && uiState.playbackStatus is PlaybackStatus.Playing,
          isFavorite = channel.id in uiState.favoriteIds,
          onSelect = { onSelectChannel(channel) },
          onToggleFavorite = { onToggleFavorite(channel.id) },
        )
      }
    }
  }
}
