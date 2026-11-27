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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.IptvViewModel
import com.example.ui.screens.IptvScreen
import com.example.ui.theme.OndeTvTheme

class MainActivity : ComponentActivity() {
  private var iptvViewModel: IptvViewModel? = null

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

    val viewModel = ViewModelProvider(this)[IptvViewModel::class.java]
    iptvViewModel = viewModel

    setContent {
      OndeTvTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
          viewModel.playerManager.pause()
        }

        LifecycleEventEffect(Lifecycle.Event.ON_START) {
          viewModel.playerManager.resumeIfReady()
        }

        LaunchedEffect(uiState.isFullscreen) {
          val insetsController = WindowCompat.getInsetsController(window, window.decorView)
          insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
          if (uiState.isFullscreen) {
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
          onSearchQueryChange = viewModel::setSearchQuery,
          onOpenImportDialog = viewModel::openImportDialog,
          onCloseImportDialog = viewModel::closeImportDialog,
          onSyncAllSources = viewModel::syncAllRemoteSources,
          onImportFromUrl = viewModel::importFromUrl,
          onImportFromText = viewModel::importFromText,
          onResetDefaultPlaylist = viewModel::resetToDefaultPlaylist,
          onClearFeedbackBanner = viewModel::clearFeedbackBanner,
          onToggleHideBroken = viewModel::toggleHideBrokenChannels,
          onCycleSortMode = viewModel::cycleSortMode,
        )
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val vm = iptvViewModel
    if (vm != null && event.action == KeyEvent.ACTION_DOWN) {
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
