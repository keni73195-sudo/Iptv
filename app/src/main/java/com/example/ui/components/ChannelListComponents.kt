package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.PlaybackStatus
import com.example.ui.theme.ActiveCard
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ControlSurface
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanBroadcast
import com.example.ui.theme.CyanBroadcastDim
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg

@Composable
fun BroadcastTopHeaderAndSearchBar(
  searchQuery: String,
  selectedCategory: ChannelCategory,
  channelCounts: Map<ChannelCategory, Int>,
  filteredCount: Int,
  totalCount: Int,
  isSyncingSources: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onCategorySelect: (ChannelCategory) -> Unit,
  onOpenImport: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isSearchFocused by remember { mutableStateOf(false) }

  Surface(
    color = ObsidianBg.copy(alpha = 0.96f),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Top Broadcast Brand Row + Persistent Search Bar + M3U Sources Pill
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Brand Badge
        Surface(
          color = CyanBroadcast.copy(alpha = 0.14f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, CyanBroadcast.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_live_tv),
              contentDescription = stringResource(R.string.app_name),
              tint = CyanBroadcast,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = InkPrimary,
            )
          }
        }

        // Top Search Bar
        Surface(
          color = ControlSurface,
          shape = RoundedCornerShape(12.dp),
          border =
            BorderStroke(
              width = if (isSearchFocused || searchQuery.isNotEmpty()) 1.5.dp else 1.dp,
              color =
                if (isSearchFocused || searchQuery.isNotEmpty()) {
                  CyanBroadcast.copy(alpha = 0.8f)
                } else {
                  BorderSlate
                },
            ),
          modifier = Modifier.weight(1f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
              tint = if (isSearchFocused || searchQuery.isNotEmpty()) CyanBroadcast else InkSecondary,
              modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
              if (searchQuery.isEmpty()) {
                Text(
                  text = stringResource(R.string.search_placeholder),
                  style = MaterialTheme.typography.bodyMedium,
                  color = InkMuted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = InkPrimary),
                cursorBrush = SolidColor(CyanBroadcast),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isSearchFocused = it.isFocused }
                    .testTag("channel_search_input"),
              )
            }
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.size(24.dp).testTag("clear_search_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Clear,
                  contentDescription = stringResource(R.string.cd_clear_search),
                  tint = InkSecondary,
                  modifier = Modifier.size(16.dp),
                )
              }
            } else {
              Surface(
                color = ElevatedCard,
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = if (filteredCount == totalCount) "$totalCount" else "$filteredCount/$totalCount",
                  style = MaterialTheme.typography.labelSmall,
                  color = InkSecondary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
        }

        // M3U Import / Sources Action Button
        var isImportFocused by remember { mutableStateOf(false) }
        Surface(
          color = if (isImportFocused) CyanBroadcast.copy(alpha = 0.22f) else ElevatedCard,
          shape = RoundedCornerShape(12.dp),
          border =
            BorderStroke(
              width = if (isImportFocused) 2.dp else 1.dp,
              color = if (isImportFocused) CyanBroadcast else BorderSlate,
            ),
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(12.dp))
              .onFocusChanged { isImportFocused = it.isFocused }
              .clickable(onClick = onOpenImport)
              .testTag("import_playlist_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (isSyncingSources) {
              CircularProgressIndicator(
                color = CyanBroadcast,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
              )
            } else {
              Icon(
                painter = painterResource(R.drawable.ic_playlist_add),
                contentDescription = stringResource(R.string.cd_import_playlist),
                tint = CyanBroadcast,
                modifier = Modifier.size(18.dp),
              )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "M3U",
              style = MaterialTheme.typography.labelMedium,
              color = InkPrimary,
            )
          }
        }
      }

      // Category Filter Chips Strip
      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(ChannelCategory.entries) { category ->
          val selected = category == selectedCategory
          val count = channelCounts[category] ?: 0
          val label = categoryLabel(category)
          val iconRes = categoryIconRes(category)
          var isChipFocused by remember { mutableStateOf(false) }

          Surface(
            color =
              when {
                selected -> CyanBroadcast.copy(alpha = 0.18f)
                isChipFocused -> ElevatedCard
                else -> ControlSurface
              },
            shape = RoundedCornerShape(999.dp),
            border =
              BorderStroke(
                width = if (isChipFocused) 2.dp else 1.dp,
                color = if (selected || isChipFocused) CyanBroadcast else BorderSlate,
              ),
            modifier =
              Modifier
                .clip(RoundedCornerShape(999.dp))
                .onFocusChanged { isChipFocused = it.isFocused }
                .clickable { onCategorySelect(category) }
                .testTag("filter_chip_${category.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected || isChipFocused) CyanBroadcast else InkSecondary,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (selected || isChipFocused) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (selected || isChipFocused) CyanBroadcast else InkSecondary,
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected || isChipFocused) CyanBroadcast else InkMuted,
              )
            }
          }
        }
      }

      Spacer(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderSlate.copy(alpha = 0.65f))
      )
    }
  }
}

@Composable
fun ActiveChannelTelemetryStrip(
  activeChannel: Channel?,
  totalChannelCount: Int,
  playbackStatus: PlaybackStatus,
  isSyncingSources: Boolean,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ControlSurface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, BorderSlate),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = CyanBroadcast.copy(alpha = 0.14f),
            shape = RoundedCornerShape(4.dp),
          ) {
            Text(
              text = stringResource(R.string.now_playing_label),
              style = MaterialTheme.typography.labelSmall,
              color = CyanBroadcast,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = activeChannel?.displayTitle ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val streamKind =
          when {
            activeChannel?.streamUrl?.contains(".mpd", ignoreCase = true) == true -> "DASH"
            activeChannel?.streamUrl?.contains(".m3u8", ignoreCase = true) == true -> "HLS"
            else -> "LIVE"
          }
        val statusText =
          when {
            isSyncingSources -> stringResource(R.string.syncing_sources_status)
            playbackStatus is PlaybackStatus.Playing -> "$streamKind • Lecture fluide"
            playbackStatus is PlaybackStatus.Buffering -> stringResource(R.string.buffering_status)
            playbackStatus is PlaybackStatus.Paused -> stringResource(R.string.paused_status)
            playbackStatus is PlaybackStatus.Error -> stringResource(R.string.error_playback_title)
            else -> stringResource(R.string.channels_count_format, totalChannelCount)
          }
        val statusColor =
          when {
            isSyncingSources -> CyanBroadcast
            playbackStatus is PlaybackStatus.Playing -> EmeraldReady
            playbackStatus is PlaybackStatus.Error -> CrimsonLive
            playbackStatus is PlaybackStatus.Buffering -> CyanBroadcast
            else -> InkSecondary
          }
        val secondaryTag =
          activeChannel?.groupTitle?.takeIf { it.isNotBlank() }
            ?: activeChannel?.tvgId?.takeIf { it.isNotBlank() && !it.equals("EPG N/A", ignoreCase = true) }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!secondaryTag.isNullOrBlank()) {
            Text(
              text = "  •  $secondaryTag",
              style = MaterialTheme.typography.labelSmall,
              color = InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (activeChannel != null) {
          Surface(
            color = ElevatedCard,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, BorderSlate),
          ) {
            Text(
              text = "CH ${activeChannel.number.toString().padStart(2, '0')}",
              style = MaterialTheme.typography.labelMedium,
              color = CyanBroadcast,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
          }
        }
        if (playbackStatus is PlaybackStatus.Error) {
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = onRetry,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("telemetry_retry_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = stringResource(R.string.retry_button),
              tint = CyanBroadcast,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChannelRowCard(
  channel: Channel,
  isSelected: Boolean,
  isPlaying: Boolean,
  isFavorite: Boolean,
  onSelect: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isCardFocused by remember { mutableStateOf(false) }

  val containerBrush =
    when {
      isSelected || isCardFocused ->
        Brush.horizontalGradient(
          colors = listOf(ActiveCard, if (isCardFocused) ElevatedCard else ControlSurface),
        )
      else ->
        Brush.horizontalGradient(
          colors = listOf(ControlSurface, ControlSurface),
        )
    }
  val borderColor =
    when {
      isCardFocused -> Color(0xFF7CEEFF)
      isSelected -> CyanBroadcast.copy(alpha = 0.85f)
      else -> BorderSlate.copy(alpha = 0.8f)
    }

  Surface(
    color = Color.Transparent,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(if (isCardFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp, borderColor),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(containerBrush)
        .onFocusChanged { isCardFocused = it.isFocused }
        .clickable(onClick = onSelect)
        .testTag("channel_card_${channel.number}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Active Left Bar Accent
      Box(
        modifier =
          Modifier
            .width(3.dp)
            .height(34.dp)
            .clip(CircleShape)
            .background(if (isSelected) CyanBroadcast else Color.Transparent)
      )

      Spacer(modifier = Modifier.width(6.dp))

      // Channel Number
      Text(
        text = channel.number.toString().padStart(2, '0'),
        style = MaterialTheme.typography.labelMedium,
        color = if (isSelected) CyanBroadcast else InkMuted,
        modifier = Modifier.width(32.dp),
      )

      // Monogram / Logo Badge
      ChannelEmblem(
        monogram = channel.monogram,
        logoUrl = channel.tvgLogo,
        accentIndex = channel.accentIndex,
        isSelected = isSelected,
      )

      Spacer(modifier = Modifier.width(12.dp))

      // Title + Metadata
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = channel.displayTitle,
          style = MaterialTheme.typography.titleMedium,
          color = if (isSelected) InkPrimary else InkPrimary.copy(alpha = 0.94f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = channel.groupTitle?.takeIf { it.isNotBlank() } ?: categoryLabel(channel.category),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) CyanBroadcast.copy(alpha = 0.9f) else InkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          val qualityBadge =
            channel.resolution
              ?: if (channel.streamUrl.contains(".mpd", ignoreCase = true)) "DASH" else null
          if (!qualityBadge.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = ElevatedCard,
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(1.dp, BorderSlate),
            ) {
              Text(
                text = qualityBadge,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) CyanBroadcast else InkSecondary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              )
            }
          }
        }
      }

      if (isSelected) {
        Spacer(modifier = Modifier.width(6.dp))
        OnAirEqualizer(isAnimating = isPlaying)
      }

      IconButton(
        onClick = onToggleFavorite,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .testTag("favorite_button_${channel.number}"),
      ) {
        Icon(
          painter = painterResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border),
          contentDescription = stringResource(R.string.cd_favorite_toggle),
          tint = if (isFavorite) AmberFavorite else InkMuted,
        )
      }
    }
  }
}

private object ChannelLogoCache {
  private val cache = LruCache<String, ImageBitmap>(120)

  fun get(url: String): ImageBitmap? = cache.get(url)

  suspend fun load(url: String): ImageBitmap? {
    cache.get(url)?.let { return it }
    if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
      return null
    }
    return withContext(Dispatchers.IO) {
      var conn: HttpURLConnection? = null
      try {
        conn =
          (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "OndeTV/1.0")
          }
        if (conn.responseCode !in 200..299) return@withContext null
        val contentType = conn.contentType?.lowercase().orEmpty()
        if (contentType.contains("svg")) return@withContext null

        val bytes =
          conn.inputStream.use { input ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
              val read = input.read(buf)
              if (read <= 0) break
              total += read
              if (total > 262_144) return@withContext null
              out.write(buf, 0, read)
            }
            out.toByteArray()
          }
        if (bytes.isEmpty()) return@withContext null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        var sample = 1
        while (bounds.outWidth / sample > 128 || bounds.outHeight / sample > 128) {
          sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
        if (bitmap != null) {
          cache.put(url, bitmap)
        }
        bitmap
      } catch (_: Throwable) {
        null
      } finally {
        conn?.disconnect()
      }
    }
  }
}

@Composable
private fun ChannelEmblem(
  monogram: String,
  logoUrl: String?,
  accentIndex: Int,
  isSelected: Boolean,
) {
  val palette =
    listOf(
      Color(0xFF1E3A8A) to Color(0xFF0EA5E9),
      Color(0xFF115E59) to Color(0xFF14B8A6),
      Color(0xFF3730A3) to Color(0xFF6366F1),
      Color(0xFF7C2D12) to Color(0xFFF97316),
      Color(0xFF1E293B) to Color(0xFF38BDF8),
      Color(0xFF581C87) to Color(0xFFA855F7),
    )
  val (startColor, endColor) = palette[accentIndex % palette.size]

  var logoBitmap by remember(logoUrl) {
    mutableStateOf(logoUrl?.let { ChannelLogoCache.get(it) })
  }

  LaunchedEffect(logoUrl) {
    if (!logoUrl.isNullOrBlank() && logoBitmap == null) {
      logoBitmap = ChannelLogoCache.load(logoUrl)
    }
  }

  Box(
    modifier =
      Modifier
        .size(46.dp)
        .clip(RoundedCornerShape(11.dp))
        .background(
          Brush.linearGradient(
            colors =
              if (isSelected) {
                listOf(CyanBroadcastDim, CyanBroadcast.copy(alpha = 0.65f))
              } else {
                listOf(startColor, endColor.copy(alpha = 0.55f))
              }
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    val loaded = logoBitmap
    if (loaded == null) {
      Text(
        text = monogram,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = InkPrimary,
      )
    } else {
      Box(
        modifier =
          Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          bitmap = loaded,
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(34.dp).padding(2.dp),
        )
      }
    }
  }
}

@Composable
private fun OnAirEqualizer(isAnimating: Boolean) {
  val infiniteTransition = rememberInfiniteTransition(label = "eq")
  val bar1 by
    infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
      label = "eq1",
    )
  val bar2 by
    infiniteTransition.animateFloat(
      initialValue = 0.9f,
      targetValue = 0.25f,
      animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
      label = "eq2",
    )
  val bar3 by
    infiniteTransition.animateFloat(
      initialValue = 0.4f,
      targetValue = 0.95f,
      animationSpec = infiniteRepeatable(tween(510), RepeatMode.Reverse),
      label = "eq3",
    )

  Row(
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    modifier = Modifier.height(16.dp).padding(horizontal = 4.dp),
  ) {
    listOf(
        if (isAnimating) bar1 else 0.45f,
        if (isAnimating) bar2 else 0.75f,
        if (isAnimating) bar3 else 0.5f,
      )
      .forEach { fraction ->
        Box(
          modifier =
            Modifier
              .width(3.dp)
              .height((16f * fraction).dp)
              .clip(CircleShape)
              .background(CyanBroadcast)
        )
      }
  }
}

@Composable
fun EmptyChannelsState(
  isFavoritesFilter: Boolean,
  modifier: Modifier = Modifier,
  onResetFilters: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      color = ElevatedCard,
      shape = CircleShape,
      border = BorderStroke(1.dp, BorderSlate),
      modifier = Modifier.size(60.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter = painterResource(if (isFavoritesFilter) R.drawable.ic_live_tv else R.drawable.ic_tv_off),
          contentDescription = null,
          tint = CyanBroadcast,
          modifier = Modifier.size(28.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text =
        stringResource(
          if (isFavoritesFilter) R.string.empty_favorites_title else R.string.empty_search_title
        ),
      style = MaterialTheme.typography.titleMedium,
      color = InkPrimary,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text =
        stringResource(
          if (isFavoritesFilter) {
            R.string.empty_favorites_subtitle
          } else {
            R.string.empty_search_subtitle
          }
        ),
      style = MaterialTheme.typography.bodyMedium,
      color = InkSecondary,
      textAlign = TextAlign.Center,
    )
    if (onResetFilters != null) {
      Spacer(modifier = Modifier.height(14.dp))
      OutlinedButton(
        onClick = onResetFilters,
        border = BorderStroke(1.dp, CyanBroadcast.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBroadcast),
      ) {
        Text(text = stringResource(R.string.reset_filters))
      }
    }
  }
}

@Composable
private fun categoryLabel(category: ChannelCategory): String =
  when (category) {
    ChannelCategory.ALL -> stringResource(R.string.filter_all)
    ChannelCategory.FAVORITES -> stringResource(R.string.filter_favorites)
    ChannelCategory.NEWS -> stringResource(R.string.filter_news)
    ChannelCategory.SPORTS -> stringResource(R.string.filter_sports)
    ChannelCategory.MOVIES -> stringResource(R.string.filter_movies)
    ChannelCategory.KIDS -> stringResource(R.string.filter_kids)
    ChannelCategory.DOCUMENTARY -> stringResource(R.string.filter_documentary)
    ChannelCategory.MUSIC -> stringResource(R.string.filter_music)
    ChannelCategory.GENERAL -> stringResource(R.string.filter_general)
  }

@DrawableRes
private fun categoryIconRes(category: ChannelCategory): Int =
  when (category) {
    ChannelCategory.ALL -> R.drawable.ic_grid_view
    ChannelCategory.FAVORITES -> R.drawable.ic_star
    ChannelCategory.NEWS -> R.drawable.ic_newspaper
    ChannelCategory.SPORTS -> R.drawable.ic_sports_soccer
    ChannelCategory.MOVIES -> R.drawable.ic_movie
    ChannelCategory.KIDS -> R.drawable.ic_child_care
    ChannelCategory.DOCUMENTARY -> R.drawable.ic_public
    ChannelCategory.MUSIC -> R.drawable.ic_music_note
    ChannelCategory.GENERAL -> R.drawable.ic_tv
  }
