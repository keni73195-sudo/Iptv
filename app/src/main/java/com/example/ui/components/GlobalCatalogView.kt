package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.model.ChannelSortMode
import com.example.model.GlobalGroupMode
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.OrangeAccent

private data class CatalogSection(
  val key: String,
  val title: String,
  val channels: List<Channel>,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalCatalogView(
  channels: List<Channel>,
  recentChannels: List<Channel>,
  favoriteIds: Set<String>,
  brokenChannelIds: Set<String>,
  hideBrokenChannels: Boolean,
  sortMode: ChannelSortMode,
  isAdultUnlocked: Boolean,
  activeChannelId: String?,
  isPlaying: Boolean,
  onSelectChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onToggleHideBroken: () -> Unit,
  onCycleSortMode: () -> Unit,
  onRequestUnlockAdult: () -> Unit,
  onLockAdult: () -> Unit,
  onClearRecent: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var groupMode by remember { mutableStateOf(GlobalGroupMode.CATEGORY) }

  val categoryOrder =
    remember(isAdultUnlocked) {
      buildList {
        add(ChannelCategory.GENERAL)
        add(ChannelCategory.NEWS)
        add(ChannelCategory.SPORTS)
        add(ChannelCategory.MOVIES)
        add(ChannelCategory.COMEDY)
        add(ChannelCategory.ENTERTAINMENT)
        add(ChannelCategory.KIDS)
        add(ChannelCategory.DOCUMENTARY)
        add(ChannelCategory.MUSIC)
        add(ChannelCategory.RELIGIOUS)
        if (isAdultUnlocked) add(ChannelCategory.ADULT)
      }
    }

  val categoryNames =
    categoryOrder.associateWith { cat -> categoryLabel(cat) }

  val sections =
    remember(channels, brokenChannelIds, hideBrokenChannels, sortMode, groupMode, isAdultUnlocked, categoryNames) {
      val ageSafe =
        if (isAdultUnlocked) channels
        else channels.filter { it.category != ChannelCategory.ADULT }
      val visible =
        if (hideBrokenChannels) ageSafe.filter { it.id !in brokenChannelIds } else ageSafe

      fun sortSectionChannels(items: List<Channel>): List<Channel> {
        val sorted =
          when (sortMode) {
            ChannelSortMode.ALPHABETICAL -> items.sortedBy { it.displayTitle.lowercase() }
            ChannelSortMode.COUNTRY ->
              items.sortedWith(
                compareBy<Channel> { if (it.country == "International") 1 else 0 }
                  .thenBy { it.country.lowercase() }
                  .thenBy { it.displayTitle.lowercase() }
              )
            ChannelSortMode.LANGUAGE ->
              items.sortedWith(
                compareBy<Channel> { if (it.language == "Autre") 1 else 0 }
                  .thenBy { it.language.lowercase() }
                  .thenBy { it.displayTitle.lowercase() }
              )
            ChannelSortMode.CATEGORY -> items.sortedBy { it.displayTitle.lowercase() }
            ChannelSortMode.DEFAULT -> items
          }
        val (working, broken) = sorted.partition { it.id !in brokenChannelIds }
        return working + broken
      }

      when (groupMode) {
        GlobalGroupMode.CATEGORY -> {
          categoryOrder.mapNotNull { cat ->
            val items = visible.filter { it.category == cat }
            if (items.isEmpty()) null
            else {
              CatalogSection(
                key = "cat_${cat.name}",
                title = categoryNames[cat] ?: cat.name,
                channels = sortSectionChannels(items),
              )
            }
          }
        }
        GlobalGroupMode.COUNTRY -> {
          visible
            .groupBy { it.country }
            .entries
            .sortedWith(
              compareBy<Map.Entry<String, List<Channel>>> { if (it.key == "International") 1 else 0 }
                .thenByDescending { it.value.size }
                .thenBy { it.key.lowercase() }
            )
            .map { (country, items) ->
              CatalogSection(
                key = "country_$country",
                title = country,
                channels = sortSectionChannels(items),
              )
            }
        }
        GlobalGroupMode.LANGUAGE -> {
          visible
            .groupBy { it.language }
            .entries
            .sortedWith(
              compareBy<Map.Entry<String, List<Channel>>> { if (it.key == "Autre") 1 else 0 }
                .thenByDescending { it.value.size }
                .thenBy { it.key.lowercase() }
            )
            .map { (language, items) ->
              CatalogSection(
                key = "lang_$language",
                title = language,
                channels = sortSectionChannels(items),
              )
            }
        }
      }
    }

  val workingCount = channels.count { it.id !in brokenChannelIds }

  LazyColumn(
    modifier =
      modifier
        .fillMaxSize()
        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
        .testTag("global_catalog_view"),
    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Global Summary & Grouping/Filter Controls Header
    item(key = "global_summary_header") {
      Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF222838)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Classement Global",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "$workingCount chaînes actives • ${sections.size} sections • ${brokenChannelIds.size} hors service",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Grouping mode selector: Par Catégorie / Par Pays / Par Langue
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(
              GlobalGroupMode.CATEGORY to stringResource(R.string.global_group_category),
              GlobalGroupMode.COUNTRY to stringResource(R.string.global_group_country),
              GlobalGroupMode.LANGUAGE to stringResource(R.string.global_group_language),
            ).forEach { (mode, label) ->
              val isSelected = groupMode == mode
              var isFocused by remember { mutableStateOf(false) }
              Surface(
                color = if (isSelected) OrangeAccent else Color(0xFF1E2638),
                shape = RoundedCornerShape(999.dp),
                border =
                  BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color =
                      when {
                        isFocused -> Color.White
                        isSelected -> OrangeAccent
                        else -> Color(0xFF334155)
                      },
                  ),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable { groupMode = mode }
                    .testTag("global_group_${mode.name.lowercase()}"),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            var isHideFocused by remember { mutableStateOf(false) }
            Surface(
              color = if (hideBrokenChannels) OrangeAccent.copy(alpha = 0.22f) else Color(0xFF1E2638),
              shape = RoundedCornerShape(999.dp),
              border =
                BorderStroke(
                  width = if (isHideFocused) 2.dp else 1.dp,
                  color =
                    when {
                      isHideFocused -> Color.White
                      hideBrokenChannels -> OrangeAccent
                      else -> Color(0xFF334155)
                    },
                ),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .onFocusChanged { isHideFocused = it.isFocused }
                  .clickable(onClick = onToggleHideBroken)
                  .testTag("global_hide_broken_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                  text = if (hideBrokenChannels) "HS masquées" else "Masquer hors service",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White,
                )
              }
            }

            var isSortFocused by remember { mutableStateOf(false) }
            Surface(
              color = Color(0xFF1E2638),
              shape = RoundedCornerShape(999.dp),
              border =
                BorderStroke(
                  width = if (isSortFocused) 2.dp else 1.dp,
                  color = if (isSortFocused) OrangeAccent else Color(0xFF334155),
                ),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .onFocusChanged { isSortFocused = it.isFocused }
                  .clickable(onClick = onCycleSortMode)
                  .testTag("global_sort_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                  text =
                    when (sortMode) {
                      ChannelSortMode.CATEGORY -> "Tri : Catégorie"
                      ChannelSortMode.COUNTRY -> "Tri : Pays"
                      ChannelSortMode.LANGUAGE -> "Tri : Langue"
                      ChannelSortMode.ALPHABETICAL -> "Tri : A–Z"
                      ChannelSortMode.DEFAULT -> "Tri : Défaut"
                    },
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White,
                )
              }
            }

            var isAdultFocused by remember { mutableStateOf(false) }
            Surface(
              color = if (isAdultUnlocked) Color(0xFF2D1622) else Color(0xFF1E2638),
              shape = RoundedCornerShape(999.dp),
              border =
                BorderStroke(
                  width = if (isAdultFocused) 2.dp else 1.dp,
                  color =
                    when {
                      isAdultFocused -> Color.White
                      isAdultUnlocked -> Color(0xFFEF4444).copy(alpha = 0.7f)
                      else -> Color(0xFF334155)
                    },
                ),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .onFocusChanged { isAdultFocused = it.isFocused }
                  .clickable { if (isAdultUnlocked) onLockAdult() else onRequestUnlockAdult() }
                  .testTag("global_adult_toggle_button"),
            ) {
              Text(
                text =
                  if (isAdultUnlocked) stringResource(R.string.adult_lock_label)
                  else "🔒 ${stringResource(R.string.adult_unlock_label)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isAdultUnlocked) Color(0xFFFCA5A5) else Color(0xFFCBD5E1),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
          }
        }
      }
    }

    // Continue Watching Section in Global View
    if (recentChannels.isNotEmpty()) {
      item(key = "global_continue_watching") {
        ContinueWatchingSection(
          recentChannels = recentChannels,
          activeChannelId = activeChannelId,
          isPlaying = isPlaying,
          brokenChannelIds = brokenChannelIds,
          favoriteIds = favoriteIds,
          onSelectChannel = onSelectChannel,
          onToggleFavorite = onToggleFavorite,
          onClearRecent = onClearRecent,
          isCompact = false,
        )
      }
    }

    // Grouped Sections (by Category, Country, or Language)
    items(sections, key = { it.key }) { section ->
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Surface(
            color = Color(0xFF1E2433),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "${section.channels.size}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = OrangeAccent,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(section.channels, key = { "${section.key}_${it.id}" }) { channel ->
            val isBroken = channel.id in brokenChannelIds
            val isSelected = channel.id == activeChannelId
            val isFav = channel.id in favoriteIds
            var isCardFocused by remember { mutableStateOf(false) }

            Surface(
              color = if (isSelected || isCardFocused) Color.White else Color(0xFF131722),
              shape = RoundedCornerShape(14.dp),
              border =
                BorderStroke(
                  width = if (isCardFocused) 2.5.dp else 1.dp,
                  color =
                    when {
                      isCardFocused -> OrangeAccent
                      isSelected -> OrangeAccent
                      isBroken -> Color(0xFFEF4444).copy(alpha = 0.35f)
                      else -> Color(0xFF222838)
                    },
                ),
              modifier =
                Modifier
                  .width(164.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .onFocusChanged { isCardFocused = it.isFocused }
                  .combinedClickable(
                    onClick = { onSelectChannel(channel) },
                    onLongClick = { onToggleFavorite(channel.id) },
                  )
                  .testTag("global_channel_card_${channel.number}"),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier =
                      Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBroken) Color(0xFF334155) else OrangeAccent),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = channel.monogram.take(2),
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                    )
                  }

                  Icon(
                    painter = painterResource(if (isFav) R.drawable.ic_star else R.drawable.ic_star_border),
                    contentDescription = stringResource(R.string.cd_favorite_toggle),
                    tint = if (isFav) AmberFavorite else Color(0xFF64748B),
                    modifier =
                      Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onToggleFavorite(channel.id) },
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = channel.displayTitle,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (isSelected || isCardFocused) Color(0xFF111827) else if (isBroken) Color(0xFF94A3B8) else Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                val metaText =
                  when {
                    isBroken -> stringResource(R.string.channel_offline_badge)
                    groupMode == GlobalGroupMode.COUNTRY -> "${channel.language} • CH ${channel.number}"
                    groupMode == GlobalGroupMode.LANGUAGE -> "${channel.country} • CH ${channel.number}"
                    channel.country != "International" -> "${channel.country} • ${channel.language}"
                    else -> "CH ${channel.number}"
                  }

                Text(
                  text = metaText,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isBroken) Color(0xFFEF4444) else if (isSelected || isCardFocused) Color(0xFF4B5563) else Color(0xFF8A93A6),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
  }
}
