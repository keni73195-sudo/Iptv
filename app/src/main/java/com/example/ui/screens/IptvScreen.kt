package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.model.ChannelCategory
import com.example.model.PlaybackStatus
import com.example.ui.IptvUiState
import com.example.ui.components.ActiveChannelTelemetryStrip
import com.example.ui.components.ChannelRowCard
import com.example.ui.components.ChannelSearchAndFilterBar
import com.example.ui.components.EmptyChannelsState
import com.example.ui.components.ImportPlaylistDialog
import com.example.ui.components.VideoPlayerStage
import com.example.ui.theme.ObsidianBg

@Composable
fun IptvScreen(
  uiState: IptvUiState,
  player: Player,
  onSelectChannel: (com.example.model.Channel) -> Unit,
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

  LaunchedEffect(uiState.feedbackBanner) {
    val msg = uiState.feedbackBanner
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearFeedbackBanner()
    }
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
      BoxWithConstraints(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.statusBars)
      ) {
        val isWideLayout = maxWidth >= 600.dp

        if (isWideLayout) {
          // Tablet / Landscape Broadcast Control Room
          Row(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier =
                Modifier
                  .weight(0.58f)
                  .fillMaxHeight()
                  .windowInsetsPadding(WindowInsets.navigationBars)
                  .padding(12.dp)
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
                modifier = Modifier.fillMaxWidth().weight(1f),
              )
              Spacer(modifier = Modifier.height(10.dp))
              ActiveChannelTelemetryStrip(
                activeChannel = uiState.activeChannel,
                totalChannelCount = uiState.allChannels.size,
                playbackStatus = uiState.playbackStatus,
                isSyncingSources = uiState.isSyncingSources,
                onOpenImport = onOpenImportDialog,
                onRetry = onRetryPlayback,
              )
            }

            Column(
              modifier =
                Modifier
                  .weight(0.42f)
                  .fillMaxHeight()
                  .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
              ChannelSearchAndFilterBar(
                searchQuery = uiState.searchQuery,
                selectedCategory = uiState.selectedCategory,
                channelCounts = channelCounts,
                onSearchQueryChange = onSearchQueryChange,
                onCategorySelect = onCategorySelect,
              )
              ChannelListBody(
                uiState = uiState,
                onSelectChannel = onSelectChannel,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f),
              )
            }
          }
        } else {
          // Handheld Portrait Layout
          Column(modifier = Modifier.fillMaxSize()) {
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
              modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )

            ActiveChannelTelemetryStrip(
              activeChannel = uiState.activeChannel,
              totalChannelCount = uiState.allChannels.size,
              playbackStatus = uiState.playbackStatus,
              isSyncingSources = uiState.isSyncingSources,
              onOpenImport = onOpenImportDialog,
              onRetry = onRetryPlayback,
            )

            ChannelSearchAndFilterBar(
              searchQuery = uiState.searchQuery,
              selectedCategory = uiState.selectedCategory,
              channelCounts = channelCounts,
              onSearchQueryChange = onSearchQueryChange,
              onCategorySelect = onCategorySelect,
            )

            ChannelListBody(
              uiState = uiState,
              onSelectChannel = onSelectChannel,
              onToggleFavorite = onToggleFavorite,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .weight(1f)
                  .windowInsetsPadding(WindowInsets.navigationBars),
            )
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
private fun ChannelListBody(
  uiState: IptvUiState,
  onSelectChannel: (com.example.model.Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (uiState.filteredChannels.isEmpty()) {
    EmptyChannelsState(
      isFavoritesFilter = uiState.selectedCategory == ChannelCategory.FAVORITES,
      modifier = modifier,
    )
  } else {
    LazyColumn(
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = modifier,
    ) {
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
