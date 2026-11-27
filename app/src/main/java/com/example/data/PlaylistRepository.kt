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

class PlaylistRepository(
  private val context: Context,
) {
  private object Keys {
    const val PREFS_NAME = "ondetv_prefs"
    const val FAVORITE_IDS = "favorite_channel_ids"
    const val LAST_CHANNEL_ID = "last_channel_id"
    const val AUTO_SYNCED_REMOTE = "auto_synced_remote_sources_v3"
  }

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(Keys.PREFS_NAME, Context.MODE_PRIVATE)

  private val _favoriteChannelIds =
    MutableStateFlow(prefs.getStringSet(Keys.FAVORITE_IDS, emptySet())?.toSet() ?: emptySet())
  val favoriteChannelIds: Flow<Set<String>> = _favoriteChannelIds.asStateFlow()

  private val _lastChannelId = MutableStateFlow(prefs.getString(Keys.LAST_CHANNEL_ID, null))
  val lastChannelId: Flow<String?> = _lastChannelId.asStateFlow()

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
    withContext(Dispatchers.IO) {
      prefs.edit().putString(Keys.LAST_CHANNEL_ID, channelId).apply()
    }
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
              readTimeout = 30_000
              instanceFollowRedirects = true
              setRequestProperty("User-Agent", "OndeTV-Android/1.0")
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
                M3uParser.parse(body, sourceTag = source.countryTag)
              }
            }
          }
          .awaitAll()

      var successCount = 0
      val merged = mutableListOf<Channel>()
      results.forEach { res ->
        res.onSuccess { channels ->
          if (channels.isNotEmpty()) {
            successCount++
            merged.addAll(channels)
          }
        }
      }
      merged to successCount
    }
}
