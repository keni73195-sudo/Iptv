package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.data.DefaultPlaylists
import com.example.data.M3uParser
import com.example.data.PlaylistRepository
import com.example.model.AspectRatioMode
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.PlaybackStatus
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
  val activeChannel: Channel? = null,
  val selectedCategory: ChannelCategory = ChannelCategory.ALL,
  val searchQuery: String = "",
  val favoriteIds: Set<String> = emptySet(),
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
)

private data class FilterControls(
  val selectedCategory: ChannelCategory,
  val searchQuery: String,
  val aspectRatioMode: AspectRatioMode,
  val isFullscreen: Boolean,
)

private data class DialogAndSyncControls(
  val isImportDialogOpen: Boolean,
  val isImporting: Boolean,
  val isSyncingSources: Boolean,
  val importError: String?,
  val feedbackBanner: String?,
)

class IptvViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = PlaylistRepository(application.applicationContext)
  val playerManager = IptvPlayerManager(application.applicationContext)

  val exoPlayer: Player
    get() = playerManager.player

  private val _channels = MutableStateFlow<List<Channel>>(M3uParser.parse(DefaultPlaylists.BFM_M3U))
  private val _activeChannelId = MutableStateFlow<String?>(null)
  private val _selectedCategory = MutableStateFlow(ChannelCategory.ALL)
  private val _searchQuery = MutableStateFlow("")
  private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
  private val _isFullscreen = MutableStateFlow(false)
  private val _isImportDialogOpen = MutableStateFlow(false)
  private val _isImporting = MutableStateFlow(false)
  private val _isSyncingSources = MutableStateFlow(false)
  private val _importError = MutableStateFlow<String?>(null)
  private val _feedbackBanner = MutableStateFlow<String?>(null)

  private val filterControlsFlow =
    combine(_selectedCategory, _searchQuery, _aspectRatioMode, _isFullscreen) {
      category,
      query,
      aspect,
      fullscreen ->
      FilterControls(category, query, aspect, fullscreen)
    }

  private val dialogControlsFlow =
    combine(
      _isImportDialogOpen,
      _isImporting,
      _isSyncingSources,
      _importError,
      _feedbackBanner,
    ) { open, importing, syncing, error, banner ->
      DialogAndSyncControls(open, importing, syncing, error, banner)
    }

  val uiState: StateFlow<IptvUiState> =
    combine(
        _channels,
        _activeChannelId,
        repository.favoriteChannelIds,
        combine(
          playerManager.playbackStatus,
          playerManager.isMuted,
          playerManager.videoResolutionLabel,
        ) { status, muted, res ->
          Triple(status, muted, res)
        },
        combine(filterControlsFlow, dialogControlsFlow) { f, d -> f to d },
      ) { channels, activeId, favoriteIds, playerInfo, controlsPair ->
        val (filters, dialog) = controlsPair
        val activeChannel =
          channels.firstOrNull { it.id == activeId } ?: channels.firstOrNull()
        val query = filters.searchQuery.trim()
        val filtered =
          channels.filter { channel ->
            val matchesCategory =
              when (filters.selectedCategory) {
                ChannelCategory.ALL -> true
                ChannelCategory.FAVORITES -> channel.id in favoriteIds
                else -> channel.category == filters.selectedCategory
              }
            val matchesQuery =
              query.isEmpty() ||
                channel.displayTitle.contains(query, ignoreCase = true) ||
                channel.tvgId.contains(query, ignoreCase = true) ||
                channel.groupTitle?.contains(query, ignoreCase = true) == true ||
                channel.number.toString().padStart(2, '0').contains(query)
            matchesCategory && matchesQuery
          }

        IptvUiState(
          allChannels = channels,
          filteredChannels = filtered,
          activeChannel = activeChannel,
          selectedCategory = filters.selectedCategory,
          searchQuery = filters.searchQuery,
          favoriteIds = favoriteIds,
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
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
          IptvUiState(
            allChannels = _channels.value,
            filteredChannels = _channels.value,
            activeChannel = _channels.value.firstOrNull(),
          ),
      )

  init {
    viewModelScope.launch {
      val savedCustomM3u = repository.loadCustomPlaylistM3u()
      val initialChannels =
        withContext(Dispatchers.Default) {
          if (!savedCustomM3u.isNullOrBlank()) {
            val parsed = M3uParser.parse(savedCustomM3u)
            parsed.ifEmpty {
              M3uParser.parse(DefaultPlaylists.loadBundledPlaylistM3u(getApplication()))
            }
          } else {
            M3uParser.parse(DefaultPlaylists.loadBundledPlaylistM3u(getApplication()))
          }
        }
      _channels.value = initialChannels

      val lastId = repository.lastChannelId.first()
      val defaultChannel =
        initialChannels.firstOrNull { it.id == lastId }
          ?: initialChannels.firstOrNull { it.displayTitle.equals("BFM TV", ignoreCase = true) }
          ?: initialChannels.firstOrNull()

      defaultChannel?.let { selectChannel(it) }

      val alreadyAutoSynced = repository.hasAutoSyncedRemote.first()
      if (savedCustomM3u.isNullOrBlank() && !alreadyAutoSynced) {
        syncRemoteSourcesInternal(closeDialogOnSuccess = false)
      }
    }
  }

  fun selectChannel(channel: Channel) {
    _activeChannelId.value = channel.id
    playerManager.playChannel(channel)
    viewModelScope.launch { repository.saveLastChannelId(channel.id) }
  }

  fun zapNext() {
    val list = uiState.value.filteredChannels.ifEmpty { _channels.value }
    if (list.isEmpty()) return
    val currentIdx = list.indexOfFirst { it.id == _activeChannelId.value }
    val nextIdx = if (currentIdx < 0) 0 else (currentIdx + 1) % list.size
    selectChannel(list[nextIdx])
  }

  fun zapPrevious() {
    val list = uiState.value.filteredChannels.ifEmpty { _channels.value }
    if (list.isEmpty()) return
    val currentIdx = list.indexOfFirst { it.id == _activeChannelId.value }
    val prevIdx = if (currentIdx <= 0) list.lastIndex else currentIdx - 1
    selectChannel(list[prevIdx])
  }

  fun togglePlayPause() = playerManager.togglePlayPause()

  fun retryPlayback() = playerManager.retryCurrentChannel()

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
    _selectedCategory.value = category
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun toggleFavorite(channelId: String) {
    viewModelScope.launch { repository.toggleFavorite(channelId) }
  }

  fun openImportDialog() {
    _importError.value = null
    _isImportDialogOpen.value = true
  }

  fun closeImportDialog() {
    _importError.value = null
    _isImportDialogOpen.value = false
  }

  fun syncAllRemoteSources() {
    if (_isSyncingSources.value) return
    viewModelScope.launch {
      syncRemoteSourcesInternal(closeDialogOnSuccess = true)
    }
  }

  private suspend fun syncRemoteSourcesInternal(closeDialogOnSuccess: Boolean) {
    _isSyncingSources.value = true
    _importError.value = null
    val (remoteChannels, sourceCount) =
      repository.fetchAndParseSources(DefaultPlaylists.REMOTE_SOURCES)
    _isSyncingSources.value = false

    if (remoteChannels.isEmpty()) {
      if (closeDialogOnSuccess) {
        _importError.value = "Impossible de joindre les playlists distantes."
      }
      return
    }

    val merged =
      withContext(Dispatchers.Default) {
        val existing = _channels.value
        val existingUrls = existing.map { it.streamUrl }.toHashSet()
        val uniqueAdditions = mutableListOf<Channel>()
        for (channel in remoteChannels) {
          if (existingUrls.add(channel.streamUrl)) {
            uniqueAdditions.add(channel)
          }
        }
        (existing + uniqueAdditions).mapIndexed { index, ch -> ch.copy(number = index + 1) }
      }

    _channels.value = merged
    if (closeDialogOnSuccess) {
      _isImportDialogOpen.value = false
    }
    _feedbackBanner.value = "$sourceCount sources synchronisées • ${merged.size} chaînes"
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
              M3uParser.parse(body, sourceTag = matchedPreset?.countryTag)
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
        val existing = _channels.value
        val existingUrls = existing.map { it.streamUrl }.toHashSet()
        val additions = mutableListOf<Channel>()
        for (ch in parsed) {
          if (existingUrls.add(ch.streamUrl)) {
            additions.add(ch)
          }
        }
        (existing + additions).mapIndexed { index, ch -> ch.copy(number = index + 1) }
      } else {
        parsed
      }

    _channels.value = finalChannels
    _selectedCategory.value = ChannelCategory.ALL
    _searchQuery.value = ""
    _isImportDialogOpen.value = false
    _importError.value = null
    _feedbackBanner.value = "${parsed.size} chaînes chargées (${finalChannels.size} au total)"

    viewModelScope.launch { repository.saveCustomPlaylistM3u(serializeToM3u(finalChannels)) }
    if (!mergeWithCurrent || _activeChannelId.value == null) {
      finalChannels.firstOrNull()?.let { selectChannel(it) }
    }
  }

  private fun serializeToM3u(channels: List<Channel>): String =
    buildString {
      appendLine("#EXTM3U")
      channels.forEach { ch ->
        val logoAttr = ch.tvgLogo?.let { " tvg-logo=\"$it\"" }.orEmpty()
        val groupAttr = ch.groupTitle?.let { " group-title=\"$it\"" }.orEmpty()
        appendLine("#EXTINF:-1 tvg-id=\"${ch.tvgId}\"$logoAttr$groupAttr,${ch.name}")
        ch.userAgent?.let { appendLine("#EXTVLCOPT:http-user-agent=$it") }
        ch.referrer?.let { appendLine("#EXTVLCOPT:http-referrer=$it") }
        appendLine(ch.streamUrl)
      }
    }

  override fun onCleared() {
    playerManager.release()
    super.onCleared()
  }
}
