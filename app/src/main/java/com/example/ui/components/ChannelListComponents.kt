package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
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
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.ChannelSortMode
import com.example.model.PlaybackStatus
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ControlSurface
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OrangeAccent
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun rememberCurrentTimeText(): String {
  var timeText by remember { mutableStateOf(formatCurrentTime()) }
  LaunchedEffect(Unit) {
    while (true) {
      timeText = formatCurrentTime()
      delay(15_000L)
    }
  }
  return timeText
}

private fun formatCurrentTime(): String {
  val cal = Calendar.getInstance()
  return String.format(
    Locale.getDefault(),
    "%02d:%02d",
    cal.get(Calendar.HOUR_OF_DAY),
    cal.get(Calendar.MINUTE),
  )
}

@Composable
fun BroadcastTopHeaderAndSearchBar(
  searchQuery: String,
  selectedTab: String,
  filteredCount: Int,
  totalCount: Int,
  isSyncingSources: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onTabSelected: (String) -> Unit,
  onOpenImport: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isSearchExpanded by remember { mutableStateOf(false) }
  var isSearchFocused by remember { mutableStateOf(false) }

  Surface(
    color = ObsidianBg.copy(alpha = 0.98f),
    modifier = modifier.fillMaxWidth(),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val isCompactHeader = maxWidth < 680.dp

      Column(modifier = Modifier.fillMaxWidth()) {
        if (isCompactHeader) {
          // Phone / Compact layout: 2 tidy rows so nothing is clipped or hidden
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            BrandTitle()

            HeaderRightActions(
              isCompact = true,
              isSearchActive = isSearchExpanded || searchQuery.isNotEmpty(),
              isSyncingSources = isSyncingSources,
              onToggleSearch = { isSearchExpanded = !isSearchExpanded },
              onOpenImport = onOpenImport,
            )
          }

          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
          ) {
            NavigationTabsCapsule(
              selectedTab = selectedTab,
              isCompact = true,
              onTabSelected = onTabSelected,
            )
          }
        } else {
          // Wide / TV / Tablet layout: single horizontal row matching reference screenshot
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            BrandTitle()

            NavigationTabsCapsule(
              selectedTab = selectedTab,
              isCompact = false,
              onTabSelected = onTabSelected,
            )

            HeaderRightActions(
              isCompact = false,
              isSearchActive = isSearchExpanded || searchQuery.isNotEmpty(),
              isSyncingSources = isSyncingSources,
              onToggleSearch = { isSearchExpanded = !isSearchExpanded },
              onOpenImport = onOpenImport,
            )
          }
        }

        // Inline Search Bar when expanded or active
        if (isSearchExpanded || searchQuery.isNotEmpty()) {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = ControlSurface,
              shape = RoundedCornerShape(12.dp),
              border =
                BorderStroke(
                  width = 1.dp,
                  color = if (isSearchFocused) OrangeAccent else BorderSlate,
                ),
              modifier = Modifier.weight(1f),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Search,
                  contentDescription = null,
                  tint = if (isSearchFocused) OrangeAccent else InkSecondary,
                  modifier = Modifier.size(18.dp),
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
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    cursorBrush = SolidColor(OrangeAccent),
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
                    modifier = Modifier.size(28.dp).testTag("clear_search_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Clear,
                      contentDescription = stringResource(R.string.cd_clear_search),
                      tint = InkSecondary,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                } else {
                  Text(
                    text = "$filteredCount/$totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                  )
                }
              }
            }
          }
        }

        Spacer(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(Color(0xFF1E2433).copy(alpha = 0.55f))
        )
      }
    }
  }
}

@Composable
private fun BrandTitle() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.testTag("app_brand_header"),
  ) {
    Text(
      text = stringResource(R.string.app_name),
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = Color.White,
      maxLines = 1,
    )
    Box(
      modifier =
        Modifier
          .padding(start = 3.dp, top = 6.dp)
          .size(7.dp)
          .clip(CircleShape)
          .background(OrangeAccent)
    )
  }
}

@Composable
private fun NavigationTabsCapsule(
  selectedTab: String,
  isCompact: Boolean,
  onTabSelected: (String) -> Unit,
) {
  val tabs = listOf("TV", "Global", "Films", "Séries")
  Row(
    modifier =
      Modifier
        .horizontalScroll(rememberScrollState())
        .background(Color(0xFF131722), RoundedCornerShape(999.dp))
        .border(1.dp, Color(0xFF222838), RoundedCornerShape(999.dp))
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    tabs.forEach { tab ->
      val isSelected = selectedTab == tab
      var isTabFocused by remember { mutableStateOf(false) }
      Surface(
        color = if (isSelected) Color(0xFF262C3A) else Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border =
          if (isTabFocused) BorderStroke(1.5.dp, OrangeAccent) else null,
        modifier =
          Modifier
            .clip(RoundedCornerShape(999.dp))
            .onFocusChanged { isTabFocused = it.isFocused }
            .clickable { onTabSelected(tab) }
            .testTag("tab_$tab"),
      ) {
        Column(
          modifier =
            Modifier.padding(
              horizontal = if (isCompact) 13.dp else 16.dp,
              vertical = if (isCompact) 6.dp else 7.dp,
            ),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = tab,
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              ),
            color = if (isSelected) Color.White else Color(0xFF8A93A6),
            maxLines = 1,
          )
          if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
              modifier =
                Modifier
                  .size(4.dp)
                  .clip(CircleShape)
                  .background(OrangeAccent)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeaderRightActions(
  isCompact: Boolean,
  isSearchActive: Boolean,
  isSyncingSources: Boolean,
  onToggleSearch: () -> Unit,
  onOpenImport: () -> Unit,
) {
  var isSearchBtnFocused by remember { mutableStateOf(false) }
  var isImportBtnFocused by remember { mutableStateOf(false) }
  val timeText = rememberCurrentTimeText()

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (!isCompact) {
      Surface(
        color = Color(0xFFFF5B5B),
        shape = CircleShape,
        modifier = Modifier.size(32.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "S",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
        }
      }
    }

    // Search action toggle button
    Surface(
      color =
        when {
          isSearchBtnFocused -> OrangeAccent.copy(alpha = 0.28f)
          isSearchActive -> OrangeAccent.copy(alpha = 0.2f)
          else -> Color(0xFF131722)
        },
      shape = CircleShape,
      border =
        BorderStroke(
          width = if (isSearchBtnFocused) 2.dp else 1.dp,
          color = if (isSearchBtnFocused || isSearchActive) OrangeAccent else Color(0xFF222838),
        ),
      modifier =
        Modifier
          .size(36.dp)
          .clip(CircleShape)
          .onFocusChanged { isSearchBtnFocused = it.isFocused }
          .clickable(onClick = onToggleSearch)
          .testTag("toggle_search_button"),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = stringResource(R.string.search_placeholder),
          tint = if (isSearchBtnFocused || isSearchActive) OrangeAccent else Color(0xFF9CA3AF),
          modifier = Modifier.size(17.dp),
        )
      }
    }

    // M3U / Sources import button
    Surface(
      color = if (isImportBtnFocused) OrangeAccent.copy(alpha = 0.28f) else Color(0xFF131722),
      shape = CircleShape,
      border =
        BorderStroke(
          width = if (isImportBtnFocused) 2.dp else 1.dp,
          color = if (isImportBtnFocused) OrangeAccent else Color(0xFF222838),
        ),
      modifier =
        Modifier
          .size(36.dp)
          .clip(CircleShape)
          .onFocusChanged { isImportBtnFocused = it.isFocused }
          .clickable(onClick = onOpenImport)
          .testTag("header_m3u_button"),
    ) {
      Box(contentAlignment = Alignment.Center) {
        if (isSyncingSources) {
          CircularProgressIndicator(
            color = OrangeAccent,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp),
          )
        } else {
          Icon(
            painter = painterResource(R.drawable.ic_playlist_add),
            contentDescription = stringResource(R.string.cd_import_playlist),
            tint = if (isImportBtnFocused) OrangeAccent else Color(0xFF9CA3AF),
            modifier = Modifier.size(17.dp),
          )
        }
      }
    }

    if (!isCompact) {
      Text(
        text = timeText,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = Modifier.testTag("header_live_clock"),
      )
    }
  }
}

@Composable
fun CategoryFilterRow(
  selectedCategory: ChannelCategory,
  hideBrokenChannels: Boolean,
  sortMode: ChannelSortMode,
  brokenCount: Int,
  onCategorySelect: (ChannelCategory) -> Unit,
  onToggleHideBroken: () -> Unit,
  onCycleSortMode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val categories =
    listOf(
      ChannelCategory.ALL,
      ChannelCategory.FAVORITES,
      ChannelCategory.GENERAL,
      ChannelCategory.NEWS,
      ChannelCategory.MOVIES,
      ChannelCategory.SPORTS,
      ChannelCategory.KIDS,
      ChannelCategory.DOCUMENTARY,
      ChannelCategory.MUSIC,
    )

  LazyRow(
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().testTag("category_filter_row"),
  ) {
    // 1. Hide / Show broken channels toggle button
    item(key = "toggle_hide_broken") {
      var isFocused by remember { mutableStateOf(false) }
      Surface(
        color = if (hideBrokenChannels) OrangeAccent.copy(alpha = 0.22f) else Color(0xFF1A2030),
        shape = RoundedCornerShape(999.dp),
        border =
          BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color =
              when {
                isFocused -> Color.White
                hideBrokenChannels -> OrangeAccent
                else -> Color(0xFF334155)
              },
          ),
        modifier =
          Modifier
            .clip(RoundedCornerShape(999.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onToggleHideBroken)
            .testTag("toggle_hide_broken_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            painter = painterResource(if (hideBrokenChannels) R.drawable.ic_tv_off else R.drawable.ic_filter_list),
            contentDescription = null,
            tint = if (hideBrokenChannels) OrangeAccent else Color(0xFF9CA3AF),
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              when {
                hideBrokenChannels && brokenCount > 0 -> "HS masquées ($brokenCount)"
                hideBrokenChannels -> "HS masquées"
                brokenCount > 0 -> "Masquer HS ($brokenCount)"
                else -> "Masquer HS"
              },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (hideBrokenChannels) Color.White else Color(0xFFCBD5E1),
          )
        }
      }
    }

    // 2. Clean Sort Mode button
    item(key = "cycle_sort_mode") {
      var isFocused by remember { mutableStateOf(false) }
      val sortLabel =
        when (sortMode) {
          ChannelSortMode.CATEGORY -> "Tri : Catégorie"
          ChannelSortMode.ALPHABETICAL -> "Tri : A–Z"
          ChannelSortMode.DEFAULT -> "Tri : Défaut"
        }
      Surface(
        color = Color(0xFF1A2030),
        shape = RoundedCornerShape(999.dp),
        border =
          BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) OrangeAccent else Color(0xFF334155),
          ),
        modifier =
          Modifier
            .clip(RoundedCornerShape(999.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onCycleSortMode)
            .testTag("cycle_sort_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_sort),
            contentDescription = null,
            tint = OrangeAccent,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = sortLabel,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
          )
        }
      }
    }

    // 3. Category Filter Pills
    items(categories) { category ->
      val isSelected = category == selectedCategory
      var isChipFocused by remember { mutableStateOf(false) }

      val label =
        when (category) {
          ChannelCategory.FAVORITES -> "★ Mes chaînes"
          ChannelCategory.ALL -> "Toutes"
          ChannelCategory.GENERAL -> "Général"
          ChannelCategory.MOVIES -> "Cinéma"
          ChannelCategory.NEWS -> "Info"
          ChannelCategory.KIDS -> "Jeunesse"
          ChannelCategory.MUSIC -> "Musique"
          ChannelCategory.SPORTS -> "Sport"
          ChannelCategory.DOCUMENTARY -> "Docs"
        }

      Surface(
        color =
          when {
            isSelected -> Color(0xFF262C3A)
            isChipFocused -> Color(0xFF1E2433)
            else -> Color.Transparent
          },
        shape = RoundedCornerShape(999.dp),
        border =
          BorderStroke(
            width = if (isChipFocused) 1.5.dp else 1.dp,
            color =
              when {
                isSelected -> Color(0xFF384357)
                isChipFocused -> OrangeAccent
                else -> Color.Transparent
              },
          ),
        modifier =
          Modifier
            .clip(RoundedCornerShape(999.dp))
            .onFocusChanged { isChipFocused = it.isFocused }
            .clickable { onCategorySelect(category) }
            .testTag("category_pill_${category.name.lowercase()}"),
      ) {
        Text(
          text = label,
          style =
            MaterialTheme.typography.bodyMedium.copy(
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
          color = if (isSelected) Color.White else Color(0xFF9CA3AF),
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelRowCard(
  channel: Channel,
  isSelected: Boolean,
  isPlaying: Boolean,
  isFavorite: Boolean,
  isBroken: Boolean = false,
  isTelevision: Boolean = false,
  onSelect: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isCardFocused by remember { mutableStateOf(false) }
  val active = isSelected || isCardFocused

  val cardBg = if (active) Color.White else Color(0xFF131722).copy(alpha = 0.85f)
  val numberColor = if (active) Color(0xFF4B5563) else if (isBroken) Color(0xFF64748B) else Color(0xFF6B7280)
  val titleColor = if (active) Color(0xFF111827) else if (isBroken) Color(0xFF94A3B8) else Color.White
  val subColor = if (active) Color(0xFF4B5563) else Color(0xFF6B7280)
  val borderColor =
    when {
      isCardFocused -> OrangeAccent
      isSelected -> Color.Transparent
      else -> Color(0xFF1E2433)
    }

  Surface(
    color = cardBg,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(if (isCardFocused) 2.5.dp else 1.dp, borderColor),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .onFocusChanged { isCardFocused = it.isFocused }
        .combinedClickable(
          onClick = onSelect,
          onLongClick = onToggleFavorite,
        )
        .testTag("channel_card_${channel.number}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Channel Number
      Text(
        text = channel.number.toString(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = numberColor,
        modifier = Modifier.width(28.dp),
      )

      // Channel Logo Badge / Monogram
      ChannelEmblem(
        channelNumber = channel.number,
        monogram = channel.monogram,
        logoUrl = channel.tvgLogo,
      )

      Spacer(modifier = Modifier.width(12.dp))

      // Channel Name + Category / Offline Tag
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = channel.displayTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )

          if (isBroken) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = Color(0xFFEF4444).copy(alpha = if (active) 0.14f else 0.20f),
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
            ) {
              Text(
                text = stringResource(R.string.channel_offline_badge),
                style = MaterialTheme.typography.labelSmall,
                color = if (active) Color(0xFFDC2626) else Color(0xFFFCA5A5),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        val categoryName = categoryLabel(channel.category)
        Text(
          text = categoryName,
          style = MaterialTheme.typography.labelSmall,
          color = subColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      IconButton(
        onClick = onToggleFavorite,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .focusProperties { canFocus = !isTelevision }
            .testTag("favorite_button_${channel.number}"),
      ) {
        Icon(
          painter = painterResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border),
          contentDescription = stringResource(R.string.cd_favorite_toggle),
          tint =
            when {
              isFavorite -> AmberFavorite
              active -> Color(0xFF6B7280)
              else -> Color(0xFF475569)
            },
          modifier = Modifier.size(19.dp),
        )
      }
    }
  }
}

@Composable
fun NextProgramInfoCard(
  activeChannel: Channel?,
  playbackStatus: PlaybackStatus,
  liveResolutionLabel: String?,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onToggleFullscreen: () -> Unit,
  isCompact: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val statusLabel =
    when (playbackStatus) {
      is PlaybackStatus.Playing -> "EN DIRECT"
      is PlaybackStatus.Buffering -> stringResource(R.string.buffering_status)
      is PlaybackStatus.Paused -> stringResource(R.string.paused_status)
      is PlaybackStatus.Error -> stringResource(R.string.channel_offline_badge)
      PlaybackStatus.Idle -> "Prêt à lire"
    }
  val statusColor =
    when (playbackStatus) {
      is PlaybackStatus.Playing -> EmeraldReady
      is PlaybackStatus.Error -> Color(0xFFEF4444)
      is PlaybackStatus.Buffering -> OrangeAccent
      else -> InkSecondary
    }
  val categoryText = activeChannel?.let { categoryLabel(it.category) } ?: "TV en direct"
  val resText = liveResolutionLabel ?: activeChannel?.resolution

  Surface(
    color = Color(0xFF131722),
    shape = RoundedCornerShape(if (isCompact) 14.dp else 18.dp),
    border = BorderStroke(1.dp, Color(0xFF222838)),
    modifier = modifier.fillMaxWidth().testTag("next_program_info_card"),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = if (isCompact) 14.dp else 20.dp, vertical = if (isCompact) 10.dp else 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
          )
          Text(
            text = "  •  $categoryText",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8A93A6),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!resText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = Color(0xFF1E2638),
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = resText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = activeChannel?.displayTitle ?: stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!isCompact) {
          Text(
            text = stringResource(R.string.tv_remote_fullscreen_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            maxLines = 1,
          )
        }

        if (activeChannel != null) {
          var isFavFocused by remember { mutableStateOf(false) }
          Surface(
            color = if (isFavFocused) OrangeAccent.copy(alpha = 0.25f) else Color.Transparent,
            shape = CircleShape,
            border = if (isFavFocused) BorderStroke(2.dp, OrangeAccent) else null,
            modifier =
              Modifier
                .size(40.dp)
                .clip(CircleShape)
                .onFocusChanged { isFavFocused = it.isFocused }
                .clickable(onClick = onToggleFavorite)
                .testTag("info_favorite_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                painter = painterResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border),
                contentDescription = stringResource(R.string.cd_favorite_toggle),
                tint = if (isFavorite) AmberFavorite else if (isFavFocused) Color.White else Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }

        var isFsFocused by remember { mutableStateOf(false) }
        Surface(
          color = if (isFsFocused) OrangeAccent.copy(alpha = 0.25f) else Color.Transparent,
          shape = CircleShape,
          border = if (isFsFocused) BorderStroke(2.dp, OrangeAccent) else null,
          modifier =
            Modifier
              .size(40.dp)
              .clip(CircleShape)
              .onFocusChanged { isFsFocused = it.isFocused }
              .clickable(onClick = onToggleFullscreen)
              .testTag("info_fullscreen_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              painter = painterResource(R.drawable.ic_fullscreen),
              contentDescription = stringResource(R.string.cd_fullscreen),
              tint = if (isFsFocused) OrangeAccent else Color.White,
              modifier = Modifier.size(20.dp),
            )
          }
        }
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
            setRequestProperty("User-Agent", "RifiStream/1.0")
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
  channelNumber: Int,
  monogram: String,
  logoUrl: String?,
) {
  val badgeColor =
    when ((channelNumber - 1) % 8) {
      0 -> Color(0xFFFF6600)
      1 -> Color(0xFFE11D48)
      2 -> Color(0xFF2563EB)
      3 -> Color(0xFFD97706)
      4 -> Color(0xFF0D9488)
      5 -> Color(0xFF7C3AED)
      6 -> Color(0xFF16A34A)
      7 -> Color(0xFFDB2777)
      else -> Color(0xFFFF6600)
    }

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
        .size(38.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(badgeColor),
    contentAlignment = Alignment.Center,
  ) {
    val loaded = logoBitmap
    if (loaded == null) {
      Text(
        text = monogram.take(3),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
    } else {
      Image(
        bitmap = loaded,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(28.dp),
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
          tint = OrangeAccent,
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
      color = Color.White,
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
        border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAccent),
      ) {
        Text(text = stringResource(R.string.reset_filters))
      }
    }
  }
}

@Composable
fun categoryLabel(category: ChannelCategory): String =
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
