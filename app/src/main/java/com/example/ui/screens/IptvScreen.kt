package com.example.ui.screens

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.PlaybackStatus
import com.example.ui.IptvUiState
import com.example.ui.components.BroadcastTopHeaderAndSearchBar
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.ChannelRowCard
import com.example.ui.components.EmptyChannelsState
import com.example.ui.components.ImportPlaylistDialog
import com.example.ui.components.NextProgramInfoCard
import com.example.ui.components.VideoPlayerStage
import com.example.ui.components.VodCatalogView
import com.example.ui.theme.ObsidianBg

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
  val listState = rememberLazyListState()
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

  LaunchedEffect(uiState.selectedCategory, uiState.searchQuery) {
    listState.scrollToItem(0)
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
  var selectedTab by remember { mutableStateOf("TV") }

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
                0f to Color(0xFF0F1522),
                0.25f to ObsidianBg,
                1f to ObsidianBg,
              )
            )
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(
              if (isTelevision) {
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
              } else {
                Modifier
              }
            )
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Top Header: Brand "Rifi stream •" + Capsule Tabs + Profile / Search / Clock
          BroadcastTopHeaderAndSearchBar(
            searchQuery = uiState.searchQuery,
            selectedCategory = uiState.selectedCategory,
            selectedTab = selectedTab,
            channelCounts = channelCounts,
            filteredCount = uiState.filteredChannels.size,
            totalCount = uiState.allChannels.size,
            isSyncingSources = uiState.isSyncingSources,
            onSearchQueryChange = onSearchQueryChange,
            onCategorySelect = onCategorySelect,
            onTabSelected = { selectedTab = it },
            onOpenImport = onOpenImportDialog,
          )

          BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideLayout = maxWidth >= 600.dp || isTelevision

            when (selectedTab) {
              "Films", "Séries" -> {
                VodCatalogView(
                  onPlayMovie = onSelectChannel,
                  modifier = Modifier.fillMaxSize(),
                )
              }
              else -> {
                // TV Tab: Exactly matching Screenshot_20261127-100720_6de627b1.jpg
                if (isWideLayout) {
                  Row(
                    modifier =
                      Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                  ) {
                    // LEFT COLUMN (~42%): Category pills + Channels List
                    Column(
                      modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    ) {
                      CategoryFilterRow(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelect = onCategorySelect,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                      )

                      ChannelListLazyColumn(
                        uiState = uiState,
                        listState = listState,
                        onSelectChannel = onSelectChannel,
                        onToggleFavorite = onToggleFavorite,
                        onResetFilters = {
                          onSearchQueryChange("")
                          onCategorySelect(ChannelCategory.ALL)
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                      )
                    }

                    // RIGHT COLUMN (~58%): Video Player Preview + Next Program Info Card
                    Column(
                      modifier = Modifier.weight(0.58f).fillMaxHeight(),
                      verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(20.dp)),
                      )

                      NextProgramInfoCard(
                        activeChannel = uiState.activeChannel,
                        modifier = Modifier.fillMaxWidth(),
                      )
                    }
                  }
                } else {
                  // Portrait Layout (Handheld Phones)
                  Column(
                    modifier =
                      Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
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
                          .aspectRatio(16f / 9f)
                          .clip(RoundedCornerShape(16.dp)),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NextProgramInfoCard(
                      activeChannel = uiState.activeChannel,
                      modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CategoryFilterRow(
                      selectedCategory = uiState.selectedCategory,
                      onCategorySelect = onCategorySelect,
                      modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ChannelListLazyColumn(
                      uiState = uiState,
                      listState = listState,
                      onSelectChannel = onSelectChannel,
                      onToggleFavorite = onToggleFavorite,
                      onResetFilters = {
                        onSearchQueryChange("")
                        onCategorySelect(ChannelCategory.ALL)
                      },
                      modifier = Modifier.weight(1f).fillMaxWidth(),
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
  onSelectChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onResetFilters: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    state = listState,
    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.testTag("channel_list_lazy_column"),
  ) {
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
          isBroken = channel.id in uiState.brokenChannelIds,
          onSelect = { onSelectChannel(channel) },
          onToggleFavorite = { onToggleFavorite(channel.id) },
        )
      }
    }
  }
}
