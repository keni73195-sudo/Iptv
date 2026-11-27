package com.example.model

enum class ChannelCategory {
  ALL,
  FAVORITES,
  NEWS,
  SPORTS,
  MOVIES,
  KIDS,
  DOCUMENTARY,
  MUSIC,
  GENERAL,
}

data class Channel(
  val id: String,
  val number: Int,
  val name: String,
  val displayTitle: String,
  val resolution: String?,
  val tvgId: String,
  val tvgLogo: String?,
  val groupTitle: String?,
  val userAgent: String?,
  val referrer: String?,
  val streamUrl: String,
  val category: ChannelCategory,
  val monogram: String,
  val accentIndex: Int,
)

sealed interface PlaybackStatus {
  data object Idle : PlaybackStatus

  data object Buffering : PlaybackStatus

  data class Reconnecting(val attempt: Int, val maxAttempts: Int) : PlaybackStatus

  data object Playing : PlaybackStatus

  data object Paused : PlaybackStatus

  data class Error(val message: String, val codeName: String = "") : PlaybackStatus
}

enum class AspectRatioMode {
  FIT,
  FILL,
  ZOOM,
}

enum class ChannelSortMode {
  DEFAULT,
  CATEGORY,
  ALPHABETICAL,
}
