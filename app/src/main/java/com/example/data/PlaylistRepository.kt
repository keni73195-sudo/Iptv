package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Channel
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PlaylistRepository(
  private val context: Context,
) {
  private object Keys {
    const val PREFS_NAME = "ondetv_prefs"
    const val FAVORITE_IDS = "favorite_channel_ids"
    const val LAST_CHANNEL_ID = "last_channel_id"
    const val RECENT_CHANNEL_IDS = "recent_channel_ids"
    const val AUTO_SYNCED_REMOTE = "auto_synced_remote_sources_v4"
    const val MAX_RECENT_CHANNELS = 12
  }

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

  private val _favoriteChannelIds =
    MutableStateFlow(prefs.getStringSet(Keys.FAVORITE_IDS, emptySet())?.toSet() ?: emptySet())
  val favoriteChannelIds: Flow<Set<String>> = _favoriteChannelIds.asStateFlow()

  private val _lastChannelId = MutableStateFlow(prefs.getString(Keys.LAST_CHANNEL_ID, null))
  val lastChannelId: Flow<String?> = _lastChannelId.asStateFlow()

  private val _recentChannelIds =
    MutableStateFlow(decodeRecentIds(prefs.getString(Keys.RECENT_CHANNEL_IDS, null)))
  val recentChannelIds: Flow<List<String>> = _recentChannelIds.asStateFlow()

  private val _hasAutoSyncedRemote =
    MutableStateFlow(prefs.getBoolean(Keys.AUTO_SYNCED_REMOTE, false))
  val hasAutoSyncedRemote: Flow<Boolean> = _hasAutoSyncedRemote.asStateFlow()

  private val customPlaylistFile: File
    get() = File(context.applicationContext.filesDir, "custom_playlist.m3u")

  suspend fun toggleFavorite(channelId: String) {
    val current = _favoriteChannelIds.value
    val updated = if (channelId in current) current - channelId else current + channelId
    _favoriteChannelIds.value = updated
    withContext(Dispatchers.IO) {
      prefs.edit().putStringSet(Keys.FAVORITE_IDS, updated).apply()
    }
  }

  suspend fun saveLastChannelId(channelId: String) {
    _lastChannelId.value = channelId
    val updatedRecent =
      (listOf(channelId) + _recentChannelIds.value.filterNot { it == channelId })
        .take(Keys.MAX_RECENT_CHANNELS)
    _recentChannelIds.value = updatedRecent
    val encoded = JSONArray(updatedRecent).toString()
    withContext(Dispatchers.IO) {
      prefs.edit()
        .putString(Keys.LAST_CHANNEL_ID, channelId)
        .putString(Keys.RECENT_CHANNEL_IDS, encoded)
        .apply()
    }
  }

  suspend fun clearRecentChannels() {
    _recentChannelIds.value = emptyList()
    withContext(Dispatchers.IO) {
      prefs.edit().remove(Keys.RECENT_CHANNEL_IDS).apply()
    }
  }

  private fun decodeRecentIds(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val array = JSONArray(raw)
      buildList(array.length()) {
        for (i in 0 until array.length()) {
          val id = array.optString(i)
          if (id.isNotBlank()) add(id)
        }
      }
    }.getOrDefault(emptyList())
  }

  suspend fun setAutoSyncedRemote(synced: Boolean) {
    _hasAutoSyncedRemote.value = synced
    withContext(Dispatchers.IO) {
      prefs.edit().putBoolean(Keys.AUTO_SYNCED_REMOTE, synced).apply()
    }
  }

  suspend fun loadCustomPlaylistM3u(): String? =
    withContext(Dispatchers.IO) {
      val file = customPlaylistFile
      if (file.exists()) file.readText() else null
    }

  suspend fun saveCustomPlaylistM3u(rawM3u: String?) {
    withContext(Dispatchers.IO) {
      val file = customPlaylistFile
      if (rawM3u.isNullOrBlank()) {
        if (file.exists()) file.delete()
      } else {
        file.writeText(rawM3u)
      }
    }
  }

  suspend fun fetchRemoteM3u(url: String): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        var currentUrl = url.trim()
        var hops = 0
        while (true) {
          val connection =
            (URL(currentUrl).openConnection() as HttpURLConnection).apply {
              connectTimeout = 15_000
              readTimeout = 45_000
              instanceFollowRedirects = true
              setRequestProperty("User-Agent", "RifiStream-Android/1.0")
              setRequestProperty("Connection", "close")
            }
          try {
            val code = connection.responseCode
            if (code in 300..399 && hops < 5) {
              val location = connection.getHeaderField("Location")
              if (!location.isNullOrBlank()) {
                currentUrl = URL(URL(currentUrl), location).toString()
                hops++
                continue
              }
            }
            if (code !in 200..299) {
              throw IOException("HTTP $code")
            }
            return@runCatching connection.inputStream.bufferedReader().use { it.readText() }
          } finally {
            connection.disconnect()
          }
        }
        @Suppress("UNREACHABLE_CODE")
        ""
      }
    }

  suspend fun fetchAndParseSources(sources: List<PlaylistPreset>): Pair<List<Channel>, Int> =
    coroutineScope {
      val results =
        sources
          .map { source ->
            async(Dispatchers.IO) {
              fetchRemoteM3u(source.url).map { body ->
                M3uParser.parse(body, preset = source)
              }
            }
          }
          .awaitAll()

      var successCount = 0
      var merged = emptyList<Channel>()
      results.forEach { res ->
        res.onSuccess { channels ->
          if (channels.isNotEmpty()) {
            successCount++
            merged =
              if (merged.isEmpty()) {
                channels
              } else {
                M3uParser.mergeEnrichedChannels(merged, channels)
              }
          }
        }
      }
      merged to successCount
    }
}
