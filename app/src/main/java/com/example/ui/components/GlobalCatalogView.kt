package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.OrangeAccent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalCatalogView(
  channels: List<Channel>,
  favoriteIds: Set<String>,
  brokenChannelIds: Set<String>,
  hideBrokenChannels: Boolean,
  sortMode: ChannelSortMode,
  activeChannelId: String?,
  onSelectChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onToggleHideBroken: () -> Unit,
  onCycleSortMode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val categoryOrder =
    listOf(
      ChannelCategory.GENERAL,
      ChannelCategory.NEWS,
      ChannelCategory.MOVIES,
      ChannelCategory.SPORTS,
      ChannelCategory.KIDS,
      ChannelCategory.DOCUMENTARY,
      ChannelCategory.MUSIC,
    )

  val groupedChannels =
    remember(channels, brokenChannelIds, hideBrokenChannels, sortMode) {
      val visible =
        if (hideBrokenChannels) {
          channels.filter { it.id !in brokenChannelIds }
        } else {
          channels
        }

      categoryOrder.mapNotNull { cat ->
        val items = visible.filter { it.category == cat }
        if (items.isEmpty()) {
          null
        } else {
          val sortedItems =
            when (sortMode) {
              ChannelSortMode.ALPHABETICAL -> items.sortedBy { it.displayTitle.lowercase() }
              ChannelSortMode.CATEGORY -> items.sortedBy { it.displayTitle.lowercase() }
              ChannelSortMode.DEFAULT -> items
            }
          val (working, broken) = sortedItems.partition { it.id !in brokenChannelIds }
          cat to (working + broken)
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
    // Global Summary & Quick Controls Header
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
                text = "$workingCount chaînes actives • ${brokenChannelIds.size} hors service",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                      ChannelSortMode.ALPHABETICAL -> "Tri : A–Z"
                      ChannelSortMode.DEFAULT -> "Tri : Défaut"
                    },
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White,
                )
              }
            }
          }
        }
      }
    }

    // Categorized Sections
    items(groupedChannels, key = { it.first.name }) { (category, catChannels) ->
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = categoryLabel(category),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Surface(
            color = Color(0xFF1E2433),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "${catChannels.size}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = OrangeAccent,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(catChannels, key = { "${category.name}_${it.id}" }) { channel ->
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
                  .width(156.dp)
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

                Text(
                  text = if (isBroken) stringResource(R.string.channel_offline_badge) else "CH ${channel.number}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isBroken) Color(0xFFEF4444) else if (isSelected || isCardFocused) Color(0xFF4B5563) else Color(0xFF8A93A6),
                )
              }
            }
          }
        }
      }
    }
  }
}
