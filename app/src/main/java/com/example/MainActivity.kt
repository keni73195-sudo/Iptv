package com.example

import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AccessGateState
import com.example.model.formatRemainingDuration
import com.example.ui.AccessViewModel
import com.example.ui.IptvViewModel
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthAndActivationScreen
import com.example.ui.screens.IptvScreen
import com.example.ui.theme.OndeTvTheme

class MainActivity : ComponentActivity() {
  private var iptvViewModel: IptvViewModel? = null
  private var accessViewModelRef: AccessViewModel? = null

  private val isTelevision: Boolean by lazy {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
      packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    val accessViewModel = ViewModelProvider(this)[AccessViewModel::class.java]
    val viewModel = ViewModelProvider(this)[IptvViewModel::class.java]
    accessViewModelRef = accessViewModel
    iptvViewModel = viewModel

    setContent {
      OndeTvTheme {
        val gateState by accessViewModel.gateState.collectAsStateWithLifecycle()
        val isAdminPanelOpen by accessViewModel.isAdminPanelOpen.collectAsStateWithLifecycle()
        val adminCodes by accessViewModel.adminCodes.collectAsStateWithLifecycle()
        val adminProfiles by accessViewModel.adminProfiles.collectAsStateWithLifecycle()
        val latestGeneratedCode by accessViewModel.latestGeneratedCode.collectAsStateWithLifecycle()
        val adminActionBusy by accessViewModel.adminActionBusy.collectAsStateWithLifecycle()
        val adminBanner by accessViewModel.adminBanner.collectAsStateWithLifecycle()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val canAccessChannels = gateState is AccessGateState.Authorized && !isAdminPanelOpen

        // Immediately stop playback if expired, suspended remotely, signed out, or Admin Panel opened
        LaunchedEffect(canAccessChannels) {
          if (!canAccessChannels) {
            viewModel.forceDisconnectPlayback()
          }
        }

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
          viewModel.playerManager.pause()
        }

        LifecycleEventEffect(Lifecycle.Event.ON_START) {
          if (canAccessChannels) {
            viewModel.playerManager.resumeIfReady()
          }
        }

        LaunchedEffect(uiState.isFullscreen, canAccessChannels) {
          val insetsController = WindowCompat.getInsetsController(window, window.decorView)
          insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
          if (uiState.isFullscreen && canAccessChannels) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            if (!isTelevision) {
              requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
          } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            if (!isTelevision) {
              requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
          }
        }

        val currentGate = gateState
        val isAdminUser = (currentGate as? AccessGateState.Authorized)?.isAdmin == true

        if (isAdminPanelOpen && isAdminUser) {
          AdminPanelScreen(
            codes = adminCodes,
            profiles = adminProfiles,
            latestGeneratedCode = latestGeneratedCode,
            isBusy = adminActionBusy,
            bannerMessage = adminBanner,
            serverNowMillis = accessViewModel.serverNowMillis(),
            onGenerateCode = accessViewModel::generateCode,
            onRevokeCode = accessViewModel::revokeCode,
            onDeleteCode = accessViewModel::deleteCode,
            onToggleUserRevocation = accessViewModel::toggleUserRevocation,
            onExtendUserPlan = accessViewModel::extendUserPlan,
            onClearBanner = accessViewModel::clearAdminBanner,
            onClose = accessViewModel::closeAdminPanel,
            onSignOut = {
              viewModel.forceDisconnectPlayback()
              accessViewModel.signOut()
            },
          )
        } else {
          when (currentGate) {
            is AccessGateState.Authorized -> {
              key(currentGate.uid) {
                val badge =
                  if (currentGate.isAdmin) null
                  else formatRemainingDuration(currentGate.remainingMillis)
                val displayProfileName =
                  currentGate.profile?.profileName?.takeIf { it.isNotBlank() }
                    ?: if (currentGate.isAdmin) "Admin" else currentGate.email.substringBefore("@")

                IptvScreen(
                  uiState = uiState,
                  player = viewModel.exoPlayer,
                  onSelectChannel = viewModel::selectChannel,
                  onToggleFavorite = viewModel::toggleFavorite,
                  onTogglePlayPause = viewModel::togglePlayPause,
                  onRetryPlayback = viewModel::retryPlayback,
                  onToggleMute = viewModel::toggleMute,
                  onPreviousChannel = viewModel::zapPrevious,
                  onNextChannel = viewModel::zapNext,
                  onCycleAspectRatio = viewModel::cycleAspectRatio,
                  onToggleFullscreen = viewModel::toggleFullscreen,
                  onExitFullscreen = { viewModel.setFullscreen(false) },
                  onCategorySelect = viewModel::setCategory,
                  onCountrySelect = viewModel::setCountryFilter,
                  onLanguageSelect = viewModel::setLanguageFilter,
                  onRequestUnlockAdult = viewModel::requestUnlockAdult,
                  onConfirmUnlockAdult = viewModel::confirmUnlockAdult,
                  onLockAdult = viewModel::lockAdultContent,
                  onDismissAdultPrompt = viewModel::dismissAdultPrompt,
                  onSearchQueryChange = viewModel::setSearchQuery,
                  onOpenImportDialog = viewModel::openImportDialog,
                  onCloseImportDialog = viewModel::closeImportDialog,
                  onSyncAllSources = viewModel::syncAllRemoteSources,
                  onImportFromUrl = viewModel::importFromUrl,
                  onImportFromText = viewModel::importFromText,
                  onResetDefaultPlaylist = viewModel::resetToDefaultPlaylist,
                  onClearFeedbackBanner = viewModel::clearFeedbackBanner,
                  onClearRecentChannels = viewModel::clearRecentChannels,
                  onToggleHideBroken = viewModel::toggleHideBrokenChannels,
                  onCycleSortMode = viewModel::cycleSortMode,
                  profileName = displayProfileName,
                  remainingBadge = badge,
                  isAdmin = currentGate.isAdmin,
                  onOpenAdminPanel = accessViewModel::openAdminPanel,
                  onSignOut = {
                    viewModel.forceDisconnectPlayback()
                    accessViewModel.signOut()
                  },
                )
              }
            }

            else -> {
              AuthAndActivationScreen(
                gateState = currentGate,
                isTelevision = isTelevision,
                onToggleAuthMode = accessViewModel::toggleAuthMode,
                onSubmitEmailPassword = accessViewModel::submitEmailPassword,
                onSendPasswordReset = accessViewModel::sendPasswordReset,
                onSubmitActivationCode = accessViewModel::submitActivationCode,
                onResendAdminVerification = accessViewModel::resendAdminVerification,
                onRefreshAdminVerification = accessViewModel::refreshAdminVerification,
                onSignOut = {
                  viewModel.forceDisconnectPlayback()
                  accessViewModel.signOut()
                },
              )
            }
          }
        }
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val vm = iptvViewModel
    val accessVm = accessViewModelRef
    val isUnlocked =
      accessVm != null &&
        accessVm.gateState.value is AccessGateState.Authorized &&
        !accessVm.isAdminPanelOpen.value

    if (vm != null && isUnlocked && event.action == KeyEvent.ACTION_DOWN) {
      val state = vm.uiState.value
      when (event.keyCode) {
        KeyEvent.KEYCODE_CHANNEL_UP,
        KeyEvent.KEYCODE_PAGE_UP,
        KeyEvent.KEYCODE_MEDIA_NEXT -> {
          vm.zapNext()
          return true
        }
        KeyEvent.KEYCODE_CHANNEL_DOWN,
        KeyEvent.KEYCODE_PAGE_DOWN,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
          vm.zapPrevious()
          return true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
          vm.togglePlayPause()
          return true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY -> {
          vm.playerManager.resumeIfReady()
          return true
        }
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_STOP -> {
          vm.playerManager.pause()
          return true
        }
        KeyEvent.KEYCODE_VOLUME_MUTE -> {
          vm.toggleMute()
          return true
        }
        KeyEvent.KEYCODE_INFO -> {
          if (!state.isImportDialogOpen) {
            vm.toggleFullscreen()
            return true
          }
        }
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_SETTINGS -> {
          if (!state.isFullscreen && !state.isImportDialogOpen) {
            vm.openImportDialog()
            return true
          }
        }
        KeyEvent.KEYCODE_BOOKMARK -> {
          if (!state.isImportDialogOpen) {
            vm.toggleActiveFavorite()
            return true
          }
        }
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
          if (state.isFullscreen && !state.isImportDialogOpen) {
            vm.onRemoteDigitPressed(event.keyCode - KeyEvent.KEYCODE_0)
            return true
          }
        }
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
          if (state.isFullscreen && !state.isImportDialogOpen) {
            vm.onRemoteDigitPressed(event.keyCode - KeyEvent.KEYCODE_NUMPAD_0)
            return true
          }
        }
      }
    }
    return super.dispatchKeyEvent(event)
  }
}
