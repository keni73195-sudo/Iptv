package com.example

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
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
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.IptvViewModel
import com.example.ui.screens.IptvScreen
import com.example.ui.theme.OndeTvTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    setContent {
      OndeTvTheme {
        val viewModel: IptvViewModel = viewModel()
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
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
          } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
        )
      }
    }
  }
}
