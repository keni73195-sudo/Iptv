package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.Channel
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val Context.iptvDataStore: DataStore<Preferences> by preferencesDataStore(name = "ondetv_prefs")

class PlaylistRepository(
  private val context: Context,
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build(),
) {
  private object Keys {
    val FAVORITE_IDS = stringSetPreferencesKey("favorite_channel_ids")
    val LAST_CHANNEL_ID = stringPreferencesKey("last_channel_id")
    val AUTO_SYNCED_REMOTE = booleanPreferencesKey("auto_synced_remote_sources")
  }

  private val customPlaylistFile: File
    get() = File(context.applicationContext.filesDir, "custom_playlist.m3u")

  val favoriteChannelIds: Flow<Set<String>> =
    context.iptvDataStore.data.map { prefs -> prefs[Keys.FAVORITE_IDS] ?: emptySet() }

  val lastChannelId: Flow<String?> =
    context.iptvDataStore.data.map { prefs -> prefs[Keys.LAST_CHANNEL_ID] }

  val hasAutoSyncedRemote: Flow<Boolean> =
    context.iptvDataStore.data.map { prefs -> prefs[Keys.AUTO_SYNCED_REMOTE] ?: false }

  suspend fun toggleFavorite(channelId: String) {
    context.iptvDataStore.edit { prefs ->
      val current = prefs[Keys.FAVORITE_IDS] ?: emptySet()
      prefs[Keys.FAVORITE_IDS] =
        if (channelId in current) current - channelId else current + channelId
    }
  }

  suspend fun saveLastChannelId(channelId: String) {
    context.iptvDataStore.edit { prefs -> prefs[Keys.LAST_CHANNEL_ID] = channelId }
  }

  suspend fun setAutoSyncedRemote(synced: Boolean) {
    context.iptvDataStore.edit { prefs -> prefs[Keys.AUTO_SYNCED_REMOTE] = synced }
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
        val request =
          Request.Builder()
            .url(url.trim())
            .header("User-Agent", "OndeTV-Android/1.0")
            .build()
        httpClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}")
          }
          response.body?.string().orEmpty()
        }
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
