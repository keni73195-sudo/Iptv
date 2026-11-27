package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Channel
import com.example.model.ChannelCategory
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary

data class VodMovie(
  val id: String,
  val title: String,
  val year: String,
  val duration: String,
  val category: String,
  val badge: String,
  val synopsis: String,
  val streamUrl: String,
  val gradientStart: Color,
  val gradientEnd: Color,
)

val SAMPLE_VOD_MOVIES =
  listOf(
    VodMovie(
      id = "sintel",
      title = "S I N T E L",
      year = "2010",
      duration = "15 min",
      category = "Animation • Fantastique",
      badge = "CC BY",
      synopsis =
        "Seule dans des terres glacées, Sintel cherche le bébé dragon qu'elle avait recueilli et qu'on lui a enlevé.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
      gradientStart = Color(0xFF1E293B),
      gradientEnd = Color(0xFF0F172A),
    ),
    VodMovie(
      id = "spring",
      title = "SPRING",
      year = "2019",
      duration = "8 min",
      category = "Animation • Poétique",
      badge = "4K UHD",
      synopsis = "Une jeune bergère et son chien affrontent des esprits anciens pour célébrer le printemps.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
      gradientStart = Color(0xFF0F766E),
      gradientEnd = Color(0xFF134E4A),
    ),
    VodMovie(
      id = "wing_it",
      title = "WING IT!",
      year = "2023",
      duration = "4 min",
      category = "Comédie • Famille",
      badge = "HD",
      synopsis = "Deux ingénieurs loufoques tentent de faire décoller leur fusée artisanale.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
      gradientStart = Color(0xFF1D4ED8),
      gradientEnd = Color(0xFF1E3A8A),
    ),
    VodMovie(
      id = "charge",
      title = "CHARGE",
      year = "2022",
      duration = "3 min",
      category = "Action • Cyberpunk",
      badge = "HDR",
      synopsis = "Dans un hangar désaffecté, un robot tente de recharger sa dernière batterie vitale.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
      gradientStart = Color(0xFF312E81),
      gradientEnd = Color(0xFF1E1B4B),
    ),
    VodMovie(
      id = "sprite_fright",
      title = "SPRITE FRIGHT",
      year = "2021",
      duration = "10 min",
      category = "Comédie • Horreur 80s",
      badge = "4K",
      synopsis = "Une bande d'adolescents en randonnée dérange de petites créatures forestières voraces.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
      gradientStart = Color(0xFF047857),
      gradientEnd = Color(0xFF064E3B),
    ),
    VodMovie(
      id = "coffee_run",
      title = "COFFEE RUN",
      year = "2020",
      duration = "3 min",
      category = "Animation • Vitesse",
      badge = "HD",
      synopsis = "Une course effrénée à travers l'espace et le temps pour une simple tasse de café.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
      gradientStart = Color(0xFFC026D3),
      gradientEnd = Color(0xFF701A75),
    ),
    VodMovie(
      id = "hero",
      title = "HERO",
      year = "2018",
      duration = "4 min",
      category = "Aventure • Stylisé",
      badge = "2D/3D",
      synopsis = "Un guerrier solitaire affronte une armée ennemie pour défendre un temple sacré.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
      gradientStart = Color(0xFFB45309),
      gradientEnd = Color(0xFF78350F),
    ),
    VodMovie(
      id = "tears_of_steel",
      title = "TEARS OF STEEL",
      year = "2012",
      duration = "12 min",
      category = "Science-Fiction • VFX",
      badge = "Sci-Fi",
      synopsis = "Dans un futur post-apocalyptique à Amsterdam, des résistants tentent de reprogrammer un robot géant.",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
      gradientStart = Color(0xFF0E7490),
      gradientEnd = Color(0xFF155E75),
    ),
  )

@Composable
fun VodCatalogView(
  onPlayMovie: (Channel) -> Unit,
  modifier: Modifier = Modifier,
) {
  val heroMovie = SAMPLE_VOD_MOVIES.first()

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("vod_catalog_view"),
    contentPadding = PaddingValues(bottom = 32.dp),
  ) {
    // Hero Banner: matching Screenshot_20261127-100700_8f30a2ab.jpg
    item(key = "vod_hero_banner") {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(
              Brush.horizontalGradient(
                listOf(
                  Color(0xFF0F1420),
                  Color(0xFF1E283C),
                  Color(0xFF2E3A52),
                )
              )
            ),
      ) {
        // Subtle decorative background graphic
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors = listOf(Color(0xFF384666).copy(alpha = 0.45f), Color.Transparent),
                  radius = 800f,
                )
              )
        )

        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(start = 36.dp, end = 36.dp, top = 32.dp, bottom = 24.dp),
          verticalArrangement = Arrangement.Bottom,
        ) {
          Text(
            text = heroMovie.title,
            style =
              MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 10.sp,
              ),
            color = Color.White,
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = heroMovie.year,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = InkSecondary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
              color = Color(0xFF26334A),
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = heroMovie.badge,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = InkPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = heroMovie.duration,
              style = MaterialTheme.typography.bodyMedium,
              color = InkSecondary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = heroMovie.category,
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFFF7A00),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = heroMovie.synopsis,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCBD5E1),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.68f),
          )

          Spacer(modifier = Modifier.height(18.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
              onClick = {
                onPlayMovie(
                  Channel(
                    id = "vod_${heroMovie.id}",
                    number = 1,
                    name = heroMovie.title,
                    displayTitle = heroMovie.title,
                    resolution = heroMovie.badge,
                    tvgId = heroMovie.id,
                    tvgLogo = null,
                    groupTitle = "Films & Séries",
                    userAgent = null,
                    referrer = null,
                    streamUrl = heroMovie.streamUrl,
                    category = ChannelCategory.MOVIES,
                    monogram = "SIN",
                    accentIndex = 3,
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
              shape = RoundedCornerShape(999.dp),
              modifier = Modifier.height(44.dp).testTag("vod_hero_play_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Lecture",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              )
            }

            OutlinedButton(
              onClick = {},
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(1.dp, Color(0xFF334155)),
              modifier = Modifier.height(44.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Plus d'infos",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }

    // Section 1: Récemment ajoutés
    item(key = "vod_section_recent") {
      Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
        Text(
          text = "Récemment ajoutés",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          items(SAMPLE_VOD_MOVIES) { movie ->
            VodPosterCard(
              movie = movie,
              onClick = {
                onPlayMovie(
                  Channel(
                    id = "vod_${movie.id}",
                    number = 1,
                    name = movie.title,
                    displayTitle = movie.title,
                    resolution = movie.badge,
                    tvgId = movie.id,
                    tvgLogo = null,
                    groupTitle = "Films",
                    userAgent = null,
                    referrer = null,
                    streamUrl = movie.streamUrl,
                    category = ChannelCategory.MOVIES,
                    monogram = movie.title.take(2),
                    accentIndex = 1,
                  )
                )
              },
            )
          }
        }
      }
    }

    // Section 2: À découvrir
    item(key = "vod_section_discover") {
      Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Text(
          text = "À découvrir",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          items(SAMPLE_VOD_MOVIES.reversed()) { movie ->
            VodPosterCard(
              movie = movie,
              onClick = {
                onPlayMovie(
                  Channel(
                    id = "vod_disc_${movie.id}",
                    number = 1,
                    name = movie.title,
                    displayTitle = movie.title,
                    resolution = movie.badge,
                    tvgId = movie.id,
                    tvgLogo = null,
                    groupTitle = "À découvrir",
                    userAgent = null,
                    referrer = null,
                    streamUrl = movie.streamUrl,
                    category = ChannelCategory.MOVIES,
                    monogram = movie.title.take(2),
                    accentIndex = 2,
                  )
                )
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VodPosterCard(
  movie: VodMovie,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0xFF131722),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, Color(0xFF222838)),
    modifier =
      modifier
        .width(138.dp)
        .height(190.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag("vod_card_${movie.id}"),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(movie.gradientStart, movie.gradientEnd)
            )
          )
          .padding(10.dp),
    ) {
      Surface(
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.align(Alignment.TopEnd),
      ) {
        Text(
          text = movie.badge,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
      }

      Column(modifier = Modifier.align(Alignment.BottomStart)) {
        Text(
          text = movie.title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${movie.year} • ${movie.duration}",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF94A3B8),
        )
      }
    }
  }
}
