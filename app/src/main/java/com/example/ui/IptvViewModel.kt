package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.data.DefaultPlaylists
import com.example.data.M3uParser
import com.example.data.PlaylistPreset
import com.example.data.PlaylistRepository
import com.example.model.AspectRatioMode
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.ChannelSortMode
import com.example.model.PlaybackStatus
import com.example.model.RemotePlaylistSource
import com.example.player.IptvPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IptvUiState(
  val allChannels: List<Channel> = emptyList(),
  val filteredChannels: List<Channel> = emptyList(),
  val recentChannels: List<Channel> = emptyList(),
  val activeChannel: Channel? = null,
  val selectedCategory: ChannelCategory = ChannelCategory.ALL,
  val selectedCountry: String? = null,
  val selectedLanguage: String? = null,
  val availableCountries: List<Pair<String, Int>> = emptyList(),
  val availableLanguages: List<Pair<String, Int>> = emptyList(),
  val searchQuery: String = "",
  val favoriteIds: Set<String> = emptySet(),
  val brokenChannelIds: Set<String> = emptySet(),
  val hideBrokenChannels: Boolean = false,
  val sortMode: ChannelSortMode = ChannelSortMode.CATEGORY,
  val isAdultUnlocked: Boolean = false,
  val pendingAdultPrompt: Boolean = false,
  val playbackStatus: PlaybackStatus = PlaybackStatus.Idle,
  val isMuted: Boolean = false,
  val liveResolutionLabel: String? = null,
  val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
  val isFullscreen: Boolean = false,
  val isImportDialogOpen: Boolean = false,
  val isImporting: Boolean = false,
  val isSyncingSources: Boolean = false,
  val importError: String? = null,
  val feedbackBanner: String? = null,
  val dialedChannelInput: String? = null,
)

private data class FilterControls(
  val selectedCategory: ChannelCategory,
  val selectedCountry: String?,
  val selectedLanguage: String?,
  val searchQuery: String,
  val hideBrokenChannels: Boolean,
  val sortMode: ChannelSortMode,
  val isAdultUnlocked: Boolean,
  val aspectRatioMode: AspectRatioMode,
  val isFullscreen: Boolean,
)

private data class DialogAndSyncControls(
  val isImportDialogOpen: Boolean,
  val isImporting: Boolean,
  val isSyncingSources: Boolean,
  val importError: String?,
  val feedbackBanner: String?,
  val dialedChannelInput: String?,
  val pendingAdultPrompt: Boolean,
)

class IptvViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = PlaylistRepository(application.applicationContext)
  val playerManager = IptvPlayerManager(application.applicationContext)

  val exoPlayer: Player
    get() = playerManager.player

  private val attemptedFailoverIds = mutableSetOf<String>()
  private var anchorNextChannelId: String? = null
  private var anchorPrevChannelId: String? = null

  private val _channels = MutableStateFlow<List<Channel>>(M3uParser.parse(DefaultPlaylists.BFM_M3U))
  private val _activeChannelId = MutableStateFlow<String?>(null)
  private val _brokenChannelIds = MutableStateFlow<Set<String>>(emptySet())
  private val _hideBrokenChannels = MutableStateFlow(false)
  private val _sortMode = MutableStateFlow(ChannelSortMode.CATEGORY)
  private val _selectedCategory = MutableStateFlow(ChannelCategory.ALL)
  private val _selectedCountry = MutableStateFlow<String?>(null)
  private val _selectedLanguage = MutableStateFlow<String?>(null)
  private val _searchQuery = MutableStateFlow("")
  private val _isAdultUnlocked = MutableStateFlow(false)
  private val _pendingAdultPrompt = MutableStateFlow(false)
  private var switchCategoryToAdultOnConfirm = false
  private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
  private val _isFullscreen = MutableStateFlow(false)
  private val _isImportDialogOpen = MutableStateFlow(false)
  private val _isImporting = MutableStateFlow(false)
  private val _isSyncingSources = MutableStateFlow(false)
  private val _importError = MutableStateFlow<String?>(null)
  private val _feedbackBanner = MutableStateFlow<String?>(null)
  private val _dialedChannelInput = MutableStateFlow<String?>(null)
  private var dialTimeoutJob: kotlinx.coroutines.Job? = null
  private var remoteAdminPresets: List<PlaylistPreset> = emptyList()
  private var lastSyncedRemoteSignature: String? = null
  private var remoteSyncJob: kotlinx.coroutines.Job? = null

  private val filterControlsFlow =
    combine(
      combine(_selectedCategory, _selectedCountry, _selectedLanguage) { cat, country, lang ->
        Triple(cat, country, lang)
      },
      _searchQuery,
      combine(_hideBrokenChannels, _sortMode, _isAdultUnlocked) { hideBroken, sortMode, adult ->
        Triple(hideBroken, sortMode, adult)
      },
      combine(_aspectRatioMode, _isFullscreen) { a, f -> a to f },
    ) { taxonomy, query, options, display ->
      FilterControls(
        selectedCategory = taxonomy.first,
        selectedCountry = taxonomy.second,
        selectedLanguage = taxonomy.third,
        searchQuery = query,
        hideBrokenChannels = options.first,
        sortMode = options.second,
        isAdultUnlocked = options.third,
        aspectRatioMode = display.first,
        isFullscreen = display.second,
      )
    }

  private val dialogControlsFlow =
    combine(
      combine(_isImportDialogOpen, _isImporting, _isSyncingSources) { o, i, s -> Triple(o, i, s) },
      _importError,
      _feedbackBanner,
      combine(_dialedChannelInput, _pendingAdultPrompt) { dialed, adultPrompt -> dialed to adultPrompt },
    ) { flags, error, banner, extras ->
      DialogAndSyncControls(
        isImportDialogOpen = flags.first,
        isImporting = flags.second,
        isSyncingSources = flags.third,
        importError = error,
        feedbackBanner = banner,
        dialedChannelInput = extras.first,
        pendingAdultPrompt = extras.second,
      )
    }

  private val channelTagsFlow =
    combine(
      repository.favoriteChannelIds,
      _brokenChannelIds,
      repository.recentChannelIds,
    ) { favs, broken, recents ->
      Triple(favs, broken, recents)
    }

  val uiState: StateFlow<IptvUiState> =
    combine(
        _channels,
        _activeChannelId,
        channelTagsFlow,
        combine(
          playerManager.playbackStatus,
          playerManager.isMuted,
          playerManager.videoResolutionLabel,
        ) { status, muted, res ->
          Triple(status, muted, res)
        },
        combine(filterControlsFlow, dialogControlsFlow) { f, d -> f to d },
      ) { channels, activeId, channelTags, playerInfo, controlsPair ->
        val (filters, dialog) = controlsPair
        val (favoriteIds, brokenIds, recentIds) = channelTags
        val adultUnlocked = filters.isAdultUnlocked
        val ageSafeChannels =
          if (adultUnlocked) channels else channels.filter { it.category != ChannelCategory.ADULT }
        val channelsById = ageSafeChannels.associateBy { it.id }

        val recentChannels =
          recentIds.mapNotNull { id ->
            val ch = channelsById[id] ?: return@mapNotNull null
            if (filters.hideBrokenChannels && ch.id in brokenIds) null else ch
          }
        val activeChannel =
          ageSafeChannels.firstOrNull { it.id == activeId } ?: ageSafeChannels.firstOrNull()

        val availableCountries =
          ageSafeChannels
            .groupingBy { it.country }
            .eachCount()
            .entries
            .sortedWith(
              compareBy<Map.Entry<String, Int>> { if (it.key == "International") 1 else 0 }
                .thenByDescending { it.value }
                .thenBy { it.key.lowercase() }
            )
            .map { it.key to it.value }

        val availableLanguages =
          ageSafeChannels
            .groupingBy { it.language }
            .eachCount()
            .entries
            .sortedWith(
              compareBy<Map.Entry<String, Int>> { if (it.key == "Autre") 1 else 0 }
                .thenByDescending { it.value }
                .thenBy { it.key.lowercase() }
            )
            .map { it.key to it.value }

        val query = filters.searchQuery.trim()
        val filtered =
          ageSafeChannels.filter { channel ->
            val matchesCategory =
              when (filters.selectedCategory) {
                ChannelCategory.ALL -> true
                ChannelCategory.FAVORITES -> channel.id in favoriteIds
                ChannelCategory.ADULT -> adultUnlocked && channel.category == ChannelCategory.ADULT
                else -> channel.category == filters.selectedCategory
              }
            val matchesCountry =
              filters.selectedCountry == null ||
                channel.country.equals(filters.selectedCountry, ignoreCase = true)
            val matchesLanguage =
              filters.selectedLanguage == null ||
                channel.language.equals(filters.selectedLanguage, ignoreCase = true)
            val matchesQuery =
              query.isEmpty() ||
                channel.displayTitle.contains(query, ignoreCase = true) ||
                channel.country.contains(query, ignoreCase = true) ||
                channel.language.contains(query, ignoreCase = true) ||
                channel.tvgId.contains(query, ignoreCase = true) ||
                channel.groupTitle?.contains(query, ignoreCase = true) == true ||
                channel.number.toString().padStart(2, '0').contains(query)
            val matchesBroken = if (filters.hideBrokenChannels) channel.id !in brokenIds else true
            matchesCategory && matchesCountry && matchesLanguage && matchesQuery && matchesBroken
          }

        val sorted =
          when (filters.sortMode) {
            ChannelSortMode.DEFAULT -> filtered
            ChannelSortMode.CATEGORY ->
              filtered.sortedWith(
                compareBy<Channel> { categorySortOrder(it.category) }
                  .thenBy { it.displayTitle.lowercase() }
              )
            ChannelSortMode.COUNTRY ->
              filtered.sortedWith(
                compareBy<Channel> { if (it.country == "International") 1 else 0 }
                  .thenBy { it.country.lowercase() }
                  .thenBy { it.displayTitle.lowercase() }
              )
            ChannelSortMode.LANGUAGE ->
              filtered.sortedWith(
                compareBy<Channel> { if (it.language == "Autre") 1 else 0 }
                  .thenBy { it.language.lowercase() }
                  .thenBy { it.displayTitle.lowercase() }
              )
            ChannelSortMode.ALPHABETICAL ->
              filtered.sortedBy { it.displayTitle.lowercase() }
          }

        // Automatic reordering: non-working / broken channels are always moved to the bottom ("ira en bas")
        val orderedChannels =
          if (filters.hideBrokenChannels) {
            sorted
          } else {
            val (working, broken) = sorted.partition { it.id !in brokenIds }
            working + broken
          }

        IptvUiState(
          allChannels = ageSafeChannels,
          filteredChannels = orderedChannels,
          recentChannels = recentChannels,
          activeChannel = activeChannel,
          selectedCategory = filters.selectedCategory,
          selectedCountry = filters.selectedCountry,
          selectedLanguage = filters.selectedLanguage,
          availableCountries = availableCountries,
          availableLanguages = availableLanguages,
          searchQuery = filters.searchQuery,
          favoriteIds = favoriteIds,
          brokenChannelIds = brokenIds,
          hideBrokenChannels = filters.hideBrokenChannels,
          sortMode = filters.sortMode,
          isAdultUnlocked = adultUnlocked,
          pendingAdultPrompt = dialog.pendingAdultPrompt,
          playbackStatus = playerInfo.first,
          isMuted = playerInfo.second,
          liveResolutionLabel = playerInfo.third,
          aspectRatioMode = filters.aspectRatioMode,
          isFullscreen = filters.isFullscreen,
          isImportDialogOpen = dialog.isImportDialogOpen,
          isImporting = dialog.isImporting,
          isSyncingSources = dialog.isSyncingSources,
          importError = dialog.importError,
          feedbackBanner = dialog.feedbackBanner,
          dialedChannelInput = dialog.dialedChannelInput,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
          IptvUiState(
            allChannels = _channels.value.filter { it.category != ChannelCategory.ADULT },
            filteredChannels = _channels.value.filter { it.category != ChannelCategory.ADULT },
            activeChannel = _channels.value.firstOrNull { it.category != ChannelCategory.ADULT },
          ),
      )

  init {
    playerManager.onStreamFailure = { failedChannel -> tryFailoverBackup(failedChannel) }

    viewModelScope.launch {
      playerManager.playbackStatus.collect { status ->
        when (status) {
          is PlaybackStatus.Playing -> {
            _activeChannelId.value?.let { activeId ->
              if (activeId in _brokenChannelIds.value) {
                _brokenChannelIds.value = _brokenChannelIds.value - activeId
              }
            }
          }
          is PlaybackStatus.Error -> {
            _activeChannelId.value?.let { activeId ->
              _brokenChannelIds.value = _brokenChannelIds.value + activeId
            }
          }
          else -> Unit
        }
      }
    }

    viewModelScope.launch {
      val savedCustomM3u = repository.loadCustomPlaylistM3u()
      val alreadyAutoSynced = repository.hasAutoSyncedRemote.first()
      val initialChannels =
        withContext(Dispatchers.Default) {
          val bundledChannels =
            M3uParser.parse(DefaultPlaylists.loadBundledPlaylistM3u(getApplication()))
          if (!savedCustomM3u.isNullOrBlank()) {
            val parsed = M3uParser.parse(savedCustomM3u)
            when {
              parsed.isEmpty() -> bundledChannels
              !alreadyAutoSynced -> M3uParser.mergeEnrichedChannels(parsed, bundledChannels)
              else -> parsed
            }
          } else {
            bundledChannels
          }
        }
      _channels.value = initialChannels
      if (!alreadyAutoSynced && !savedCustomM3u.isNullOrBlank()) {
        repository.saveCustomPlaylistM3u(serializeToM3u(initialChannels))
      }

      val lastId = repository.lastChannelId.first()
      // Do not auto-play on startup. Let the user explicitly choose a channel.

      if (!alreadyAutoSynced) {
        syncRemoteSourcesInternal(closeDialogOnSuccess = false)
      }
    }
  }

  fun selectChannel(channel: Channel) {
    attemptedFailoverIds.clear()
    attemptedFailoverIds.add(channel.id)
    updateZapAnchors(channel)
    playSelectedChannel(channel)
  }

  private fun ageSafeAllChannels(): List<Channel> =
    if (_isAdultUnlocked.value) _channels.value
    else _channels.value.filter { it.category != ChannelCategory.ADULT }

  private fun updateZapAnchors(channel: Channel) {
    val currentList = uiState.value.filteredChannels.ifEmpty { ageSafeAllChannels() }
    val broken = _brokenChannelIds.value
    val idx = currentList.indexOfFirst { it.id == channel.id }
    if (idx >= 0 && currentList.size > 1) {
      anchorNextChannelId =
        (1 until currentList.size).firstNotNullOfOrNull { offset ->
          currentList[(idx + offset) % currentList.size]
            .takeIf { it.id != channel.id && it.id !in broken }
            ?.id
        }
      anchorPrevChannelId =
        (1 until currentList.size).firstNotNullOfOrNull { offset ->
          currentList[(idx - offset + currentList.size) % currentList.size]
            .takeIf { it.id != channel.id && it.id !in broken }
            ?.id
        }
    } else {
      anchorNextChannelId = null
      anchorPrevChannelId = null
    }
  }

  private fun playSelectedChannel(channel: Channel) {
    _activeChannelId.value = channel.id
    playerManager.playChannel(channel)
    viewModelScope.launch { repository.saveLastChannelId(channel.id) }
  }

  private fun tryFailoverBackup(failedChannel: Channel): Boolean {
    _brokenChannelIds.value = _brokenChannelIds.value + failedChannel.id
    attemptedFailoverIds.add(failedChannel.id)
    if (attemptedFailoverIds.size >= MAX_FAILOVER_ATTEMPTS) return false

    val scopedList = uiState.value.filteredChannels.ifEmpty { ageSafeAllChannels() }
    val excluded = attemptedFailoverIds + _brokenChannelIds.value
    val backup = findBackupChannel(scopedList, failedChannel, excluded) ?: return false
    attemptedFailoverIds.add(backup.id)
    _feedbackBanner.value = "Flux indisponible, bascule vers ${backup.displayTitle}"
    playSelectedChannel(backup)
    return true
  }

  fun zapNext() {
    val list = uiState.value.filteredChannels.ifEmpty { ageSafeAllChannels() }
    if (list.isEmpty()) return
    val broken = _brokenChannelIds.value
    val activeId = _activeChannelId.value
    val currentIdx = list.indexOfFirst { it.id == activeId }

    val target =
      if (activeId != null && (activeId in broken || currentIdx < 0)) {
        anchorNextChannelId?.let { anchorId -> list.firstOrNull { it.id == anchorId } }
          ?: list.firstOrNull { it.id !in broken }
          ?: list.first()
      } else if (currentIdx < 0) {
        list.firstOrNull { it.id !in broken } ?: list.first()
      } else {
        (1..list.size).firstNotNullOfOrNull { offset ->
          val candidate = list[(currentIdx + offset) % list.size]
          candidate.takeIf { it.id !in broken }
        } ?: list[(currentIdx + 1) % list.size]
      }

    selectChannel(target)
  }

  fun zapPrevious() {
    val list = uiState.value.filteredChannels.ifEmpty { ageSafeAllChannels() }
    if (list.isEmpty()) return
    val broken = _brokenChannelIds.value
    val activeId = _activeChannelId.value
    val currentIdx = list.indexOfFirst { it.id == activeId }

    val target =
      if (activeId != null && (activeId in broken || currentIdx < 0)) {
        anchorPrevChannelId?.let { anchorId -> list.firstOrNull { it.id == anchorId } }
          ?: list.lastOrNull { it.id !in broken }
          ?: list.last()
      } else if (currentIdx < 0) {
        list.lastOrNull { it.id !in broken } ?: list.last()
      } else {
        (1..list.size).firstNotNullOfOrNull { offset ->
          val candidate = list[(currentIdx - offset + list.size) % list.size]
          candidate.takeIf { it.id !in broken }
        } ?: list[if (currentIdx <= 0) list.lastIndex else currentIdx - 1]
      }

    selectChannel(target)
  }

  fun togglePlayPause() = playerManager.togglePlayPause()

  fun retryPlayback() {
    attemptedFailoverIds.clear()
    _activeChannelId.value
      ?.let { activeId -> _channels.value.firstOrNull { it.id == activeId } }
      ?.let { attemptedFailoverIds.add(it.id) }
    playerManager.retryCurrentChannel()
  }

  fun toggleMute() = playerManager.toggleMute()

  fun cycleAspectRatio() {
    _aspectRatioMode.value =
      when (_aspectRatioMode.value) {
        AspectRatioMode.FIT -> AspectRatioMode.FILL
        AspectRatioMode.FILL -> AspectRatioMode.ZOOM
        AspectRatioMode.ZOOM -> AspectRatioMode.FIT
      }
  }

  fun setFullscreen(fullscreen: Boolean) {
    _isFullscreen.value = fullscreen
  }

  fun toggleFullscreen() {
    _isFullscreen.value = !_isFullscreen.value
  }

  fun setCategory(category: ChannelCategory) {
    if (category == ChannelCategory.ADULT && !_isAdultUnlocked.value) {
      switchCategoryToAdultOnConfirm = true
      _pendingAdultPrompt.value = true
      return
    }
    _selectedCategory.value = category
  }

  fun setCountryFilter(country: String?) {
    _selectedCountry.value = country
  }

  fun setLanguageFilter(language: String?) {
    _selectedLanguage.value = language
  }

  fun requestUnlockAdult() {
    switchCategoryToAdultOnConfirm = false
    _pendingAdultPrompt.value = true
  }

  fun confirmUnlockAdult() {
    _isAdultUnlocked.value = true
    _pendingAdultPrompt.value = false
    if (switchCategoryToAdultOnConfirm) {
      _selectedCategory.value = ChannelCategory.ADULT
      switchCategoryToAdultOnConfirm = false
    }
    _feedbackBanner.value = "Catégorie +18 déverrouillée"
  }

  fun lockAdultContent() {
    _isAdultUnlocked.value = false
    _pendingAdultPrompt.value = false
    switchCategoryToAdultOnConfirm = false
    if (_selectedCategory.value == ChannelCategory.ADULT) {
      _selectedCategory.value = ChannelCategory.ALL
    }
    val currentActive = _channels.value.firstOrNull { it.id == _activeChannelId.value }
    if (currentActive?.category == ChannelCategory.ADULT) {
      playerManager.pause()
      _activeChannelId.value = null
    }
    _feedbackBanner.value = "Contenu +18 masqué"
  }

  fun dismissAdultPrompt() {
    _pendingAdultPrompt.value = false
    switchCategoryToAdultOnConfirm = false
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun toggleFavorite(channelId: String) {
    viewModelScope.launch { repository.toggleFavorite(channelId) }
  }

  fun clearRecentChannels() {
    viewModelScope.launch { repository.clearRecentChannels() }
    _feedbackBanner.value = "Historique de lecture effacé"
  }

  fun toggleActiveFavorite() {
    val activeId = _activeChannelId.value ?: return
    toggleFavorite(activeId)
  }

  fun onRemoteDigitPressed(digit: Int) {
    if (digit !in 0..9) return
    val current = _dialedChannelInput.value.orEmpty()
    val updated = (current + digit.toString()).takeLast(3)
    _dialedChannelInput.value = updated
    dialTimeoutJob?.cancel()
    dialTimeoutJob =
      viewModelScope.launch {
        kotlinx.coroutines.delay(1_200L)
        val number = _dialedChannelInput.value?.toIntOrNull()
        _dialedChannelInput.value = null
        if (number != null && number > 0) {
          val safeAll =
            if (_isAdultUnlocked.value) _channels.value
            else _channels.value.filter { it.category != ChannelCategory.ADULT }
          val list = uiState.value.filteredChannels.ifEmpty { safeAll }
          val match =
            list.firstOrNull { it.number == number }
              ?: safeAll.firstOrNull { it.number == number }
              ?: list.getOrNull(number - 1)
          if (match != null) {
            selectChannel(match)
          } else {
            _feedbackBanner.value = "Chaîne n°$number introuvable"
          }
        }
      }
  }

  fun toggleHideBrokenChannels() {
    _hideBrokenChannels.value = !_hideBrokenChannels.value
    _feedbackBanner.value =
      if (_hideBrokenChannels.value) "Chaînes hors service masquées" else "Toutes les chaînes affichées"
  }

  fun cycleSortMode() {
    val next =
      when (_sortMode.value) {
        ChannelSortMode.CATEGORY -> ChannelSortMode.COUNTRY
        ChannelSortMode.COUNTRY -> ChannelSortMode.LANGUAGE
        ChannelSortMode.LANGUAGE -> ChannelSortMode.ALPHABETICAL
        ChannelSortMode.ALPHABETICAL -> ChannelSortMode.DEFAULT
        ChannelSortMode.DEFAULT -> ChannelSortMode.CATEGORY
      }
    _sortMode.value = next
    _feedbackBanner.value =
      when (next) {
        ChannelSortMode.CATEGORY -> "Classement : Par catégorie"
        ChannelSortMode.COUNTRY -> "Classement : Par pays"
        ChannelSortMode.LANGUAGE -> "Classement : Par langue"
        ChannelSortMode.ALPHABETICAL -> "Classement : A → Z"
        ChannelSortMode.DEFAULT -> "Classement : Ordre d'origine"
      }
  }

  fun openImportDialog() {
    _importError.value = null
    _isImportDialogOpen.value = true
  }

  fun closeImportDialog() {
    _importError.value = null
    _isImportDialogOpen.value = false
  }

  fun onRemoteSourcesUpdated(
    remoteSources: List<RemotePlaylistSource>?,
    isAuthorized: Boolean,
  ) {
    if (!isAuthorized || remoteSources == null) return
    val presets = remoteSources.map { it.toPlaylistPreset() }
    val signature =
      remoteSources
        .sortedBy { it.id }
        .joinToString("|") {
          "${it.id}:${it.url}:${it.countryTag}:${it.forcedCategory}:${it.forcedCountry}:${it.forcedLanguage}"
        }
    if (signature == lastSyncedRemoteSignature) return

    val hadPreviousRemoteSync = lastSyncedRemoteSignature != null
    remoteAdminPresets = presets
    lastSyncedRemoteSignature = signature

    // On initial empty remote list (admin hasn't added extra sources yet), keep normal startup sync
    if (!hadPreviousRemoteSync && presets.isEmpty()) return

    remoteSyncJob?.cancel()
    remoteSyncJob =
      viewModelScope.launch {
        syncAllCatalogSources(
          extraPresets = presets,
          rebuildFromBundled = true,
          closeDialogOnSuccess = false,
          showToast = true,
        )
      }
  }

  fun syncAllRemoteSources() {
    if (_isSyncingSources.value) return
    viewModelScope.launch {
      syncRemoteSourcesInternal(closeDialogOnSuccess = true)
    }
  }

  private suspend fun syncRemoteSourcesInternal(closeDialogOnSuccess: Boolean) {
    syncAllCatalogSources(
      extraPresets = remoteAdminPresets,
      rebuildFromBundled = false,
      closeDialogOnSuccess = closeDialogOnSuccess,
      showToast = true,
    )
  }

  private suspend fun syncAllCatalogSources(
    extraPresets: List<PlaylistPreset>,
    rebuildFromBundled: Boolean,
    closeDialogOnSuccess: Boolean,
    showToast: Boolean,
  ) {
    _isSyncingSources.value = true
    _importError.value = null
    val combinedSources = DefaultPlaylists.REMOTE_SOURCES + extraPresets
    val (remoteChannels, _) = repository.fetchAndParseSources(combinedSources)
    _isSyncingSources.value = false

    if (remoteChannels.isEmpty() && !rebuildFromBundled) {
      if (closeDialogOnSuccess) {
        _importError.value = "Impossible de joindre les playlists distantes."
      }
      return
    }

    val merged =
      withContext(Dispatchers.Default) {
        val base =
          if (rebuildFromBundled) {
            M3uParser.parse(DefaultPlaylists.loadBundledPlaylistM3u(getApplication()))
          } else {
            _channels.value
          }
        if (remoteChannels.isEmpty()) base
        else M3uParser.mergeEnrichedChannels(base, remoteChannels)
      }

    _channels.value = merged
    if (closeDialogOnSuccess) {
      _isImportDialogOpen.value = false
    }
    if (showToast) {
      _feedbackBanner.value = "Catalogue synchronisé • ${merged.size} chaînes"
    }
    repository.setAutoSyncedRemote(true)
    repository.saveCustomPlaylistM3u(serializeToM3u(merged))
  }

  fun importFromText(rawM3u: String, mergeWithCurrent: Boolean) {
    viewModelScope.launch {
      _isImporting.value = true
      _importError.value = null
      val parsed = withContext(Dispatchers.Default) { M3uParser.parse(rawM3u) }
      _isImporting.value = false
      if (parsed.isEmpty()) {
        _importError.value = "Aucun flux valide trouvé dans ce texte #EXTM3U."
      } else {
        applyImportedChannels(parsed, mergeWithCurrent)
      }
    }
  }

  fun importFromUrl(url: String, mergeWithCurrent: Boolean) {
    val trimmed = url.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true) &&
      !trimmed.startsWith("https://", ignoreCase = true)
    ) {
      _importError.value = "Entrez une URL commençant par http:// ou https://"
      return
    }

    val matchedPreset = DefaultPlaylists.ALL_PRESETS.firstOrNull { it.url == trimmed }
    viewModelScope.launch {
      _isImporting.value = true
      _importError.value = null
      val result = repository.fetchRemoteM3u(trimmed)
      result.fold(
        onSuccess = { body ->
          val parsed =
            withContext(Dispatchers.Default) {
              M3uParser.parse(body, preset = matchedPreset)
            }
          _isImporting.value = false
          if (parsed.isEmpty()) {
            _importError.value = "Aucun flux valide trouvé à cette adresse."
          } else {
            applyImportedChannels(parsed, mergeWithCurrent)
          }
        },
        onFailure = { error ->
          _isImporting.value = false
          _importError.value = "Échec du téléchargement : ${error.localizedMessage ?: "erreur réseau"}"
        },
      )
    }
  }

  fun resetToDefaultPlaylist() {
    viewModelScope.launch {
      val defaultChannels =
        withContext(Dispatchers.Default) {
          M3uParser.parse(DefaultPlaylists.loadBundledPlaylistM3u(getApplication()))
        }
      _channels.value = defaultChannels
      _selectedCategory.value = ChannelCategory.ALL
      _selectedCountry.value = null
      _selectedLanguage.value = null
      _searchQuery.value = ""
      _isImportDialogOpen.value = false
      _importError.value = null
      _feedbackBanner.value = "Liste locale restaurée (${defaultChannels.size} chaînes)"
      repository.setAutoSyncedRemote(true)
      repository.saveCustomPlaylistM3u(null)
      val bfmTv =
        defaultChannels.firstOrNull { it.displayTitle.equals("BFM TV", ignoreCase = true) }
          ?: defaultChannels.firstOrNull()
      bfmTv?.let { selectChannel(it) }
    }
  }

  fun clearFeedbackBanner() {
    _feedbackBanner.value = null
  }

  private fun applyImportedChannels(parsed: List<Channel>, mergeWithCurrent: Boolean) {
    val finalChannels =
      if (mergeWithCurrent) {
        M3uParser.mergeEnrichedChannels(_channels.value, parsed)
      } else {
        parsed
      }

    _channels.value = finalChannels
    _selectedCategory.value = ChannelCategory.ALL
    _selectedCountry.value = null
    _selectedLanguage.value = null
    _searchQuery.value = ""
    _isImportDialogOpen.value = false
    _importError.value = null
    _feedbackBanner.value = "${parsed.size} chaînes chargées (${finalChannels.size} au total)"

    viewModelScope.launch { repository.saveCustomPlaylistM3u(serializeToM3u(finalChannels)) }
    if (!mergeWithCurrent || _activeChannelId.value == null) {
      finalChannels.firstOrNull { _isAdultUnlocked.value || it.category != ChannelCategory.ADULT }
        ?.let { selectChannel(it) }
    }
  }

  private fun serializeToM3u(channels: List<Channel>): String =
    buildString {
      appendLine("#EXTM3U")
      channels.forEach { ch ->
        val safeId = ch.tvgId.replace("\"", "'")
        val logoAttr = ch.tvgLogo?.let { " tvg-logo=\"${it.replace("\"", "'")}\"" }.orEmpty()
        val groupAttr = ch.groupTitle?.let { " group-title=\"${it.replace("\"", "'")}\"" }.orEmpty()
        val countryAttr = " tvg-country=\"${ch.country.replace("\"", "'")}\""
        val languageAttr = " tvg-language=\"${ch.language.replace("\"", "'")}\""
        val categoryAttr = " x-category=\"${ch.category.name}\""
        appendLine("#EXTINF:-1 tvg-id=\"$safeId\"$logoAttr$groupAttr$countryAttr$languageAttr$categoryAttr,${ch.name}")
        ch.userAgent?.let { appendLine("#EXTVLCOPT:http-user-agent=$it") }
        ch.referrer?.let { appendLine("#EXTVLCOPT:http-referrer=$it") }
        appendLine(ch.streamUrl)
      }
    }

  fun forceDisconnectPlayback() {
    _isFullscreen.value = false
    _isImportDialogOpen.value = false
    _pendingAdultPrompt.value = false
    _isAdultUnlocked.value = false
    lastSyncedRemoteSignature = null
    remoteSyncJob?.cancel()
    playerManager.stopPlaybackAndClear()
  }

  override fun onCleared() {
    playerManager.release()
    super.onCleared()
  }

  companion object {
    private const val MAX_FAILOVER_ATTEMPTS = 2
    private val qualitySuffixRegex =
      Regex("""\b(FHD|UHD|HD|SD|4K|H265|HEVC|FR)\b""", RegexOption.IGNORE_CASE)

    private fun categorySortOrder(category: ChannelCategory): Int =
      when (category) {
        ChannelCategory.GENERAL -> 0
        ChannelCategory.NEWS -> 1
        ChannelCategory.SPORTS -> 2
        ChannelCategory.MOVIES -> 3
        ChannelCategory.COMEDY -> 4
        ChannelCategory.ENTERTAINMENT -> 5
        ChannelCategory.KIDS -> 6
        ChannelCategory.DOCUMENTARY -> 7
        ChannelCategory.MUSIC -> 8
        ChannelCategory.RELIGIOUS -> 9
        ChannelCategory.ADULT -> 10
        else -> 11
      }

    internal fun normalizeChannelKey(title: String): String =
      title
        .replace(Regex("^FR\\s*[:\\-]\\s*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\([^)]*\\)"), "")
        .replace(qualitySuffixRegex, "")
        .replace(Regex("[^a-zA-Z0-9]+"), "")
        .lowercase()

    internal fun findBackupChannel(
      allChannels: List<Channel>,
      failedChannel: Channel,
      excludedIds: Set<String>,
    ): Channel? {
      val targetKey = normalizeChannelKey(failedChannel.displayTitle)
      if (targetKey.length < 3) return null

      val candidates =
        allChannels.filter { candidate ->
          if (candidate.id in excludedIds || candidate.streamUrl == failedChannel.streamUrl) {
            return@filter false
          }
          candidate.category == failedChannel.category &&
            normalizeChannelKey(candidate.displayTitle) == targetKey
        }

      return candidates.firstOrNull { it.streamUrl.contains(".m3u8", ignoreCase = true) }
        ?: candidates.firstOrNull()
    }
  }
}
