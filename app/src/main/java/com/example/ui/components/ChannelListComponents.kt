package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.PlaybackStatus
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ControlSurface
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanBroadcast
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.OrangeLive
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BroadcastTopHeaderAndSearchBar(
  searchQuery: String,
  selectedCategory: ChannelCategory,
  selectedTab: String,
  channelCounts: Map<ChannelCategory, Int>,
  filteredCount: Int,
  totalCount: Int,
  isSyncingSources: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onCategorySelect: (ChannelCategory) -> Unit,
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
    Column(modifier = Modifier.fillMaxWidth()) {
      // Top Rifi stream Header Row: Brand + Navigation Tabs + Profile / Search / Clock
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Brand: Rifi stream •
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.testTag("app_brand_header"),
        ) {
          Text(
            text = "Rifi stream",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
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

        // Top Navigation Capsule: TV, Guide, Films, Séries
        Row(
          modifier =
            Modifier
              .background(Color(0xFF131722), RoundedCornerShape(999.dp))
              .border(1.dp, Color(0xFF222838), RoundedCornerShape(999.dp))
              .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val tabs = listOf("TV", "Guide", "Films", "Séries")
          tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Surface(
              color = if (isSelected) Color(0xFF262C3A) else Color.Transparent,
              shape = RoundedCornerShape(999.dp),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .clickable { onTabSelected(tab) }
                  .testTag("tab_$tab"),
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = tab,
                  style =
                    MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                  color = if (isSelected) Color.White else Color(0xFF8A93A6),
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

        // Right side accessories: Profile [S] + Red dot + Search + Sun + Live Clock
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Profile S badge
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

          // Red notification dot
          Box(
            modifier =
              Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B30))
          )

          // Search action toggle button
          Surface(
            color = if (isSearchExpanded || searchQuery.isNotEmpty()) OrangeAccent.copy(alpha = 0.2f) else Color(0xFF131722),
            shape = CircleShape,
            border = BorderStroke(1.dp, if (isSearchExpanded || searchQuery.isNotEmpty()) OrangeAccent else Color(0xFF222838)),
            modifier =
              Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { isSearchExpanded = !isSearchExpanded }
                .testTag("toggle_search_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.search_placeholder),
                tint = if (isSearchExpanded || searchQuery.isNotEmpty()) OrangeAccent else Color(0xFF9CA3AF),
                modifier = Modifier.size(16.dp),
              )
            }
          }

          // Sun / Display mode icon
          Surface(
            color = Color(0xFF131722),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color(0xFF222838)),
            modifier =
              Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenImport)
                .testTag("header_m3u_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                painter = painterResource(R.drawable.ic_public),
                contentDescription = stringResource(R.string.cd_import_playlist),
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(15.dp),
              )
            }
          }

          // Live Clock (HH:mm)
          val timeText = remember {
            val cal = Calendar.getInstance()
            String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
          }
          Text(
            text = timeText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
          )
        }
      }

      // Inline Search Bar when expanded or active
      if (isSearchExpanded || searchQuery.isNotEmpty()) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 4.dp),
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
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
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
                  modifier = Modifier.size(22.dp).testTag("clear_search_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = InkSecondary,
                    modifier = Modifier.size(15.dp),
                  )
                }
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
            .background(Color(0xFF1E2433).copy(alpha = 0.5f))
      )
    }
  }
}

@Composable
fun CategoryFilterRow(
  selectedCategory: ChannelCategory,
  onCategorySelect: (ChannelCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val categories =
    listOf(
      ChannelCategory.FAVORITES,
      ChannelCategory.ALL,
      ChannelCategory.MOVIES,
      ChannelCategory.NEWS,
      ChannelCategory.KIDS,
      ChannelCategory.MUSIC,
      ChannelCategory.SPORTS,
      ChannelCategory.DOCUMENTARY,
    )

  LazyRow(
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().testTag("category_filter_row"),
  ) {
    items(categories) { category ->
      val isSelected = category == selectedCategory
      var isChipFocused by remember { mutableStateOf(false) }

      val label =
        when (category) {
          ChannelCategory.FAVORITES -> "★ Mes chaînes"
          ChannelCategory.ALL -> "Toutes"
          ChannelCategory.MOVIES -> "Cinéma"
          ChannelCategory.NEWS -> "Info"
          ChannelCategory.KIDS -> "Jeunesse"
          ChannelCategory.MUSIC -> "Musique"
          ChannelCategory.SPORTS -> "Sport"
          ChannelCategory.DOCUMENTARY -> "Docs"
          else -> category.name
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

@Composable
fun ChannelRowCard(
  channel: Channel,
  isSelected: Boolean,
  isPlaying: Boolean,
  isFavorite: Boolean,
  isBroken: Boolean = false,
  onSelect: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isCardFocused by remember { mutableStateOf(false) }
  val active = isSelected || isCardFocused

  // In Screenshot_20261127-100720_6de627b1.jpg, active channel is solid pure white capsule!
  val cardBg = if (active) Color.White else Color(0xFF131722).copy(alpha = 0.85f)
  val numberColor = if (active) Color(0xFF4B5563) else if (isBroken) Color(0xFF64748B) else Color(0xFF6B7280)
  val titleColor = if (active) Color(0xFF111827) else if (isBroken) Color(0xFF94A3B8) else Color.White
  val borderColor = if (isCardFocused && !isSelected) OrangeAccent else if (active) Color.Transparent else Color(0xFF1E2433)

  Surface(
    color = cardBg,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(if (isCardFocused && !isSelected) 2.dp else 1.dp, borderColor),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .onFocusChanged { isCardFocused = it.isFocused }
        .clickable(onClick = onSelect)
        .testTag("channel_card_${channel.number}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Channel Number (e.g. 1, 2, 3...)
      Text(
        text = channel.number.toString(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = numberColor,
        modifier = Modifier.width(28.dp),
      )

      // Channel Logo Badge / Monogram
      ChannelEmblem(
        channelNumber = channel.number,
        displayTitle = channel.displayTitle,
        monogram = channel.monogram,
        logoUrl = channel.tvgLogo,
        category = channel.category,
        isSelected = active,
      )

      Spacer(modifier = Modifier.width(12.dp))

      // Channel Name + Offline Tag
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
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

      if (isFavorite) {
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          painter = painterResource(R.drawable.ic_star),
          contentDescription = stringResource(R.string.cd_favorite_toggle),
          tint = if (active) Color(0xFFF59E0B) else AmberFavorite,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
fun NextProgramInfoCard(
  activeChannel: Channel?,
  modifier: Modifier = Modifier,
) {
  val programTitle =
    remember(activeChannel?.id) {
      when (activeChannel?.category) {
        ChannelCategory.MOVIES -> "Le Journal du cinéma & Grand Film"
        ChannelCategory.NEWS -> "Le Journal de la nuit"
        ChannelCategory.SPORTS -> "Le Grand Soir Football"
        ChannelCategory.KIDS -> "Les Aventures du soir"
        ChannelCategory.DOCUMENTARY -> "Planète Sauvage • Épisode 3"
        ChannelCategory.MUSIC -> "Top Hits & Live Concert"
        else -> "Le Journal de la nuit"
      }
    }

  Surface(
    color = Color(0xFF131722),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, Color(0xFF222838)),
    modifier = modifier.fillMaxWidth().testTag("next_program_info_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Ensuite • 23:25",
          style = MaterialTheme.typography.labelMedium,
          color = Color(0xFF8A93A6),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = programTitle,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = "OK plein écran  •  Maintenir OK : Mes chaînes",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF6B7280),
      )
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
  displayTitle: String,
  monogram: String,
  logoUrl: String?,
  category: ChannelCategory,
  isSelected: Boolean,
) {
  // Vibrant palette matching the reference screenshot:
  // Channel 1: Orange, Channel 2: Crimson/Red, Channel 3: Blue, Channel 4: Amber, Channel 5: Teal, etc.
  val badgeColor =
    when ((channelNumber - 1) % 8) {
      0 -> Color(0xFFFF6600) // Vibrant orange (like Lumen 1 in screenshot)
      1 -> Color(0xFFE11D48) // Ciné Club Red
      2 -> Color(0xFF2563EB) // Info Blue
      3 -> Color(0xFFD97706) // Kids Amber
      4 -> Color(0xFF0D9488) // Docs Teal
      5 -> Color(0xFF7C3AED) // Musique Purple
      6 -> Color(0xFF16A34A) // Nature Green
      7 -> Color(0xFFDB2777) // Séries Pink
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
  }
}
