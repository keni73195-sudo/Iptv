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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.R
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.PlaybackStatus
import com.example.ui.IptvUiState
import com.example.ui.components.AdultConfirmationDialog
import com.example.ui.components.BroadcastTopHeaderAndSearchBar
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.ChannelRowCard
import com.example.ui.components.ContinueWatchingSection
import com.example.ui.components.CountryLanguagePickerDialog
import com.example.ui.components.EmptyChannelsState
import com.example.ui.components.GlobalCatalogView
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
  onCountrySelect: (String?) -> Unit,
  onLanguageSelect: (String?) -> Unit,
  onRequestUnlockAdult: () -> Unit,
  onConfirmUnlockAdult: () -> Unit,
  onLockAdult: () -> Unit,
  onDismissAdultPrompt: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onToggleHideBroken: () -> Unit,
  onCycleSortMode: () -> Unit,
  onOpenImportDialog: () -> Unit,
  onCloseImportDialog: () -> Unit,
  onSyncAllSources: () -> Unit,
  onImportFromUrl: (String, Boolean) -> Unit,
  onImportFromText: (String, Boolean) -> Unit,
  onResetDefaultPlaylist: () -> Unit,
  onClearFeedbackBanner: () -> Unit,
  onClearRecentChannels: () -> Unit,
  profileName: String? = null,
  remainingBadge: String? = null,
  isAdmin: Boolean = false,
  onOpenAdminPanel: (() -> Unit)? = null,
  onSignOut: (() -> Unit)? = null,
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

  LaunchedEffect(
    uiState.selectedCategory,
    uiState.selectedCountry,
    uiState.selectedLanguage,
    uiState.isAdultUnlocked,
    uiState.searchQuery,
    uiState.sortMode,
    uiState.hideBrokenChannels,
  ) {
    listState.scrollToItem(0)
  }

  val activeIsFavorite = uiState.activeChannel?.id?.let { it in uiState.favoriteIds } == true
  var selectedTab by remember { mutableStateOf("TV") }
  var isCountryPickerOpen by remember { mutableStateOf(false) }
  var isLanguagePickerOpen by remember { mutableStateOf(false) }
  val channelListFocusRequester = remember { FocusRequester() }

  LaunchedEffect(isTelevision, uiState.isFullscreen, selectedTab) {
    if (isTelevision && !uiState.isFullscreen && selectedTab == "TV" && uiState.filteredChannels.isNotEmpty()) {
      kotlinx.coroutines.delay(120L)
      runCatching { channelListFocusRequester.requestFocus() }
    }
  }

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
        isTelevision = isTelevision,
        dialedChannelInput = uiState.dialedChannelInput,
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
    BackHandler(enabled = selectedTab != "TV") {
      selectedTab = "TV"
    }
    BackHandler(enabled = selectedTab == "TV" && uiState.searchQuery.isNotEmpty()) {
      onSearchQueryChange("")
    }
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
          BroadcastTopHeaderAndSearchBar(
            searchQuery = uiState.searchQuery,
            selectedTab = selectedTab,
            filteredCount = uiState.filteredChannels.size,
            totalCount = uiState.allChannels.size,
            isSyncingSources = uiState.isSyncingSources,
            profileName = profileName,
            remainingBadge = remainingBadge,
            isAdmin = isAdmin,
            onSearchQueryChange = onSearchQueryChange,
            onTabSelected = { selectedTab = it },
            onOpenImport = onOpenImportDialog,
            onOpenAdminPanel = onOpenAdminPanel,
            onSignOut = onSignOut,
          )

          BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideLayout = maxWidth >= 600.dp || isTelevision

            when (selectedTab) {
              "Global" -> {
                GlobalCatalogView(
                  channels = uiState.allChannels,
                  recentChannels = uiState.recentChannels,
                  favoriteIds = uiState.favoriteIds,
                  brokenChannelIds = uiState.brokenChannelIds,
                  hideBrokenChannels = uiState.hideBrokenChannels,
                  sortMode = uiState.sortMode,
                  isAdultUnlocked = uiState.isAdultUnlocked,
                  activeChannelId = uiState.activeChannel?.id,
                  isPlaying = uiState.playbackStatus is PlaybackStatus.Playing,
                  onSelectChannel = { channel ->
                    selectedTab = "TV"
                    onSelectChannel(channel)
                  },
                  onToggleFavorite = onToggleFavorite,
                  onToggleHideBroken = onToggleHideBroken,
                  onCycleSortMode = onCycleSortMode,
                  onRequestUnlockAdult = onRequestUnlockAdult,
                  onLockAdult = onLockAdult,
                  onClearRecent = onClearRecentChannels,
                  modifier = Modifier.fillMaxSize(),
                )
              }
              "Films", "Séries" -> {
                VodCatalogView(
                  onPlayMovie = { channel ->
                    selectedTab = "TV"
                    onSelectChannel(channel)
                  },
                  modifier = Modifier.fillMaxSize(),
                )
              }
              else -> {
                if (isWideLayout) {
                  Row(
                    modifier =
                      Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                  ) {
                    // LEFT COLUMN (~42%): Sort/Filter/Category pills + Channels List
                    Column(
                      modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    ) {
                      CategoryFilterRow(
                        selectedCategory = uiState.selectedCategory,
                        selectedCountry = uiState.selectedCountry,
                        selectedLanguage = uiState.selectedLanguage,
                        isAdultUnlocked = uiState.isAdultUnlocked,
                        hideBrokenChannels = uiState.hideBrokenChannels,
                        sortMode = uiState.sortMode,
                        brokenCount = uiState.brokenChannelIds.size,
                        onCategorySelect = onCategorySelect,
                        onOpenCountryPicker = { isCountryPickerOpen = true },
                        onOpenLanguagePicker = { isLanguagePickerOpen = true },
                        onLockAdult = onLockAdult,
                        onToggleHideBroken = onToggleHideBroken,
                        onCycleSortMode = onCycleSortMode,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                      )

                      ChannelListLazyColumn(
                        uiState = uiState,
                        listState = listState,
                        isTelevision = isTelevision,
                        onSelectChannel = onSelectChannel,
                        onToggleFavorite = onToggleFavorite,
                        onResetFilters = {
                          onSearchQueryChange("")
                          onCategorySelect(ChannelCategory.ALL)
                          onCountrySelect(null)
                          onLanguageSelect(null)
                          if (uiState.hideBrokenChannels) onToggleHideBroken()
                        },
                        modifier =
                          Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusRequester(channelListFocusRequester),
                      )
                    }

                    // RIGHT COLUMN (~58%): Video Player Preview + Channel Info Card + Continue Watching
                    Column(
                      modifier = Modifier.weight(0.58f).fillMaxHeight(),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Box(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        contentAlignment = Alignment.TopCenter,
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
                          isTelevision = isTelevision,
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
                              .aspectRatio(16f / 9f)
                              .clip(RoundedCornerShape(20.dp)),
                        )
                      }

                      NextProgramInfoCard(
                        activeChannel = uiState.activeChannel,
                        playbackStatus = uiState.playbackStatus,
                        liveResolutionLabel = uiState.liveResolutionLabel,
                        isFavorite = activeIsFavorite,
                        onToggleFavorite = { uiState.activeChannel?.let { onToggleFavorite(it.id) } },
                        onToggleFullscreen = onToggleFullscreen,
                        isCompact = false,
                        modifier = Modifier.fillMaxWidth(),
                      )

                      if (uiState.recentChannels.isNotEmpty()) {
                        ContinueWatchingSection(
                          recentChannels = uiState.recentChannels,
                          activeChannelId = uiState.activeChannel?.id,
                          isPlaying = uiState.playbackStatus is PlaybackStatus.Playing,
                          brokenChannelIds = uiState.brokenChannelIds,
                          favoriteIds = uiState.favoriteIds,
                          onSelectChannel = onSelectChannel,
                          onToggleFavorite = onToggleFavorite,
                          onClearRecent = onClearRecentChannels,
                          isCompact = false,
                          modifier = Modifier.fillMaxWidth(),
                        )
                      }
                    }
                  }
                } else {
                  // Portrait Layout (Handheld Phones) — fully responsive so nothing is clipped
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
                          .heightIn(max = 205.dp)
                          .aspectRatio(16f / 9f)
                          .clip(RoundedCornerShape(16.dp)),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NextProgramInfoCard(
                      activeChannel = uiState.activeChannel,
                      playbackStatus = uiState.playbackStatus,
                      liveResolutionLabel = uiState.liveResolutionLabel,
                      isFavorite = activeIsFavorite,
                      onToggleFavorite = { uiState.activeChannel?.let { onToggleFavorite(it.id) } },
                      onToggleFullscreen = onToggleFullscreen,
                      isCompact = true,
                      modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    CategoryFilterRow(
                      selectedCategory = uiState.selectedCategory,
                      selectedCountry = uiState.selectedCountry,
                      selectedLanguage = uiState.selectedLanguage,
                      isAdultUnlocked = uiState.isAdultUnlocked,
                      hideBrokenChannels = uiState.hideBrokenChannels,
                      sortMode = uiState.sortMode,
                      brokenCount = uiState.brokenChannelIds.size,
                      onCategorySelect = onCategorySelect,
                      onOpenCountryPicker = { isCountryPickerOpen = true },
                      onOpenLanguagePicker = { isLanguagePickerOpen = true },
                      onLockAdult = onLockAdult,
                      onToggleHideBroken = onToggleHideBroken,
                      onCycleSortMode = onCycleSortMode,
                      modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ChannelListLazyColumn(
                      uiState = uiState,
                      listState = listState,
                      isTelevision = isTelevision,
                      showContinueWatchingHeader = true,
                      onClearRecentChannels = onClearRecentChannels,
                      onSelectChannel = onSelectChannel,
                      onToggleFavorite = onToggleFavorite,
                      onResetFilters = {
                        onSearchQueryChange("")
                        onCategorySelect(ChannelCategory.ALL)
                        onCountrySelect(null)
                        onLanguageSelect(null)
                        if (uiState.hideBrokenChannels) onToggleHideBroken()
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

  if (isAdmin && uiState.isImportDialogOpen) {
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

  if (isCountryPickerOpen) {
    CountryLanguagePickerDialog(
      title = stringResource(R.string.picker_country_title),
      allOptionLabel = stringResource(R.string.filter_country_all),
      selectedValue = uiState.selectedCountry,
      options = uiState.availableCountries,
      onSelect = onCountrySelect,
      onDismiss = { isCountryPickerOpen = false },
    )
  }

  if (isLanguagePickerOpen) {
    CountryLanguagePickerDialog(
      title = stringResource(R.string.picker_language_title),
      allOptionLabel = stringResource(R.string.filter_language_all),
      selectedValue = uiState.selectedLanguage,
      options = uiState.availableLanguages,
      onSelect = onLanguageSelect,
      onDismiss = { isLanguagePickerOpen = false },
    )
  }

  if (uiState.pendingAdultPrompt) {
    AdultConfirmationDialog(
      onConfirm = onConfirmUnlockAdult,
      onDismiss = onDismissAdultPrompt,
    )
  }
}

@Composable
private fun ChannelListLazyColumn(
  uiState: IptvUiState,
  listState: LazyListState,
  isTelevision: Boolean,
  onSelectChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onResetFilters: () -> Unit,
  modifier: Modifier = Modifier,
  showContinueWatchingHeader: Boolean = false,
  onClearRecentChannels: () -> Unit = {},
) {
  LazyColumn(
    state = listState,
    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.testTag("channel_list_lazy_column"),
  ) {
    if (showContinueWatchingHeader && uiState.recentChannels.isNotEmpty() && uiState.searchQuery.isBlank()) {
      item(key = "portrait_continue_watching") {
        ContinueWatchingSection(
          recentChannels = uiState.recentChannels,
          activeChannelId = uiState.activeChannel?.id,
          isPlaying = uiState.playbackStatus is PlaybackStatus.Playing,
          brokenChannelIds = uiState.brokenChannelIds,
          favoriteIds = uiState.favoriteIds,
          onSelectChannel = onSelectChannel,
          onToggleFavorite = onToggleFavorite,
          onClearRecent = onClearRecentChannels,
          isCompact = true,
          modifier = Modifier.padding(bottom = 4.dp),
        )
      }
    }

    if (uiState.filteredChannels.isEmpty()) {
      item(key = "empty_state") {
        EmptyChannelsState(
          isFavoritesFilter = uiState.selectedCategory == ChannelCategory.FAVORITES,
          onResetFilters =
            if (uiState.searchQuery.isNotBlank() ||
              uiState.selectedCategory != ChannelCategory.ALL ||
              uiState.selectedCountry != null ||
              uiState.selectedLanguage != null ||
              uiState.hideBrokenChannels
            ) {
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
          isTelevision = isTelevision,
          onSelect = { onSelectChannel(channel) },
          onToggleFavorite = { onToggleFavorite(channel.id) },
        )
      }
    }
  }
}
