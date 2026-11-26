package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.TvOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
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

@Composable
fun ActiveChannelTelemetryStrip(
  activeChannel: Channel?,
  totalChannelCount: Int,
  playbackStatus: PlaybackStatus,
  isSyncingSources: Boolean,
  onOpenImport: () -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ControlSurface,
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
          Text(
            text = activeChannel?.displayTitle ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (activeChannel != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "CH ${activeChannel.number.toString().padStart(2, '0')} / $totalChannelCount",
              style = MaterialTheme.typography.labelMedium,
              color = CyanBroadcast,
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        val streamKind =
          if (activeChannel?.streamUrl?.contains(".m3u8", ignoreCase = true) == true) "HLS" else "IPTV"
        val statusText =
          when {
            isSyncingSources -> stringResource(R.string.syncing_sources_status)
            playbackStatus is PlaybackStatus.Playing -> "$streamKind • Flux actif"
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
          activeChannel?.tvgId?.takeIf { it.isNotBlank() }
            ?: activeChannel?.groupTitle?.takeIf { it.isNotBlank() }
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
        if (playbackStatus is PlaybackStatus.Error) {
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
        Surface(
          color = ElevatedCard,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, BorderSlate),
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onOpenImport)
              .testTag("import_playlist_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.PlaylistAdd,
              contentDescription = stringResource(R.string.cd_import_playlist),
              tint = CyanBroadcast,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "M3U",
              style = MaterialTheme.typography.labelMedium,
              color = InkPrimary,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChannelSearchAndFilterBar(
  searchQuery: String,
  selectedCategory: ChannelCategory,
  channelCounts: Map<ChannelCategory, Int>,
  onSearchQueryChange: (String) -> Unit,
  onCategorySelect: (ChannelCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Surface(
      color = ControlSurface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, BorderSlate),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = null,
          tint = InkSecondary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
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
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkPrimary),
            cursorBrush = SolidColor(CyanBroadcast),
            modifier = Modifier.fillMaxWidth().testTag("channel_search_input"),
          )
        }
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { onSearchQueryChange("") },
            modifier = Modifier.size(28.dp).testTag("clear_search_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Clear,
              contentDescription = stringResource(R.string.cd_clear_search),
              tint = InkSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(ChannelCategory.entries) { category ->
        val selected = category == selectedCategory
        val count = channelCounts[category] ?: 0
        val label = categoryLabel(category)

        Surface(
          color = if (selected) CyanBroadcast.copy(alpha = 0.16f) else ControlSurface,
          shape = RoundedCornerShape(999.dp),
          border =
            BorderStroke(
              1.dp,
              if (selected) CyanBroadcast else BorderSlate,
            ),
          modifier =
            Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable { onCategorySelect(category) }
              .testTag("filter_chip_${category.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
              ),
              color = if (selected) CyanBroadcast else InkSecondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = count.toString(),
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) CyanBroadcast else InkMuted,
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
  val containerColor = if (isSelected) ActiveCard else ControlSurface
  val borderColor = if (isSelected) CyanBroadcast.copy(alpha = 0.75f) else BorderSlate

  Surface(
    color = containerColor,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onSelect)
        .testTag("channel_card_${channel.number}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Channel Number
      Text(
        text = channel.number.toString().padStart(2, '0'),
        style = MaterialTheme.typography.labelMedium,
        color = if (isSelected) CyanBroadcast else InkMuted,
        modifier = Modifier.width(26.dp),
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
          color = if (isSelected) InkPrimary else InkPrimary.copy(alpha = 0.92f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = channel.groupTitle?.takeIf { it.isNotBlank() } ?: categoryLabel(channel.category),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (!channel.resolution.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = ElevatedCard,
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(1.dp, BorderSlate),
            ) {
              Text(
                text = channel.resolution,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) CyanBroadcast else InkSecondary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              )
            }
          }
        }
      }

      if (isSelected) {
        Spacer(modifier = Modifier.width(8.dp))
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
          imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
          contentDescription = stringResource(R.string.cd_favorite_toggle),
          tint = if (isFavorite) AmberFavorite else InkMuted,
        )
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

  var logoLoaded by remember(logoUrl) { mutableStateOf(false) }

  Box(
    modifier =
      Modifier
        .size(44.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(
          Brush.linearGradient(
            colors =
              if (isSelected) {
                listOf(CyanBroadcastDim, CyanBroadcast.copy(alpha = 0.6f))
              } else {
                listOf(startColor, endColor.copy(alpha = 0.55f))
              }
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (!logoLoaded) {
      Text(
        text = monogram,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = InkPrimary,
      )
    }
    if (!logoUrl.isNullOrBlank()) {
      AsyncImage(
        model = logoUrl,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        onSuccess = { logoLoaded = true },
        onError = { logoLoaded = false },
        modifier = Modifier.size(36.dp).padding(2.dp),
      )
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
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      color = ElevatedCard,
      shape = CircleShape,
      border = BorderStroke(1.dp, BorderSlate),
      modifier = Modifier.size(64.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = if (isFavoritesFilter) Icons.Rounded.LiveTv else Icons.Rounded.TvOff,
          contentDescription = null,
          tint = CyanBroadcast,
          modifier = Modifier.size(30.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(14.dp))
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
