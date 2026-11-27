package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChannelCategory

data class PlaylistPreset(
  val id: String,
  val label: String,
  val countryTag: String,
  val url: String,
  val forcedCategory: ChannelCategory? = null,
  val forcedCountry: String? = null,
  val forcedLanguage: String? = null,
  val isLanguageGrouped: Boolean = false,
  val isCountryGrouped: Boolean = false,
)

object DefaultPlaylists {
  const val BFM_M3U = """#EXTM3U
#EXTINF:-1 tvg-id="BFM2.fr@HD",BFM 2 (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM2/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMAlsace.fr@SD",BFM Alsace (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_ALSACE/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMBusiness.fr@SD",BFM Business (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_BUSINESS/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMDICIAlpesduSud.fr@SD",BFM DICI Alpes du Sud (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_DICI_ALPESDUSUD/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMDICIHauteProvence.fr@SD",BFM DICI Haute-Provence (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_DICI_HAUTEPROVENCE/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMGrandLille.fr@SD",BFM Grand Lille (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFMGRANDLILLE/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMGrandLittoral.fr@SD",BFM Grand Littoral (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFMGRANDLITTORAL/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="",BFM Grands Reportages (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_GRANDSREPORTAGES/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMLyon.fr@SD",BFM Lyon (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_LYON/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMMarseille.fr@SD",BFM Marseille (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_MARSEILLEPROV/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMCotedAzur.fr@SD",BFM Nice Côte d'Azur (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_NICECOTEDAZUR/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMNormandie.fr@SD",BFM Normandie (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_NORMANDIE/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMTechCo.fr@SD",BFM Tech & Co (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_TECHANDCO/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMVar.fr@SD",BFM Toulon Var (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_TOULONVAR/index.m3u8?end=END&start=LIVE
#EXTINF:-1 tvg-id="BFMTV.fr@SD",BFM TV (1080p)
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36
https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_TV/index.m3u8?end=END&start=LIVE"""

  fun loadBundledPlaylistM3u(context: Context): String {
    val rawPlaylists =
      listOf(R.raw.fr_channels_playlist, R.raw.fr_multi_connection_playlist).map { resId ->
        runCatching {
          context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        }
          .getOrDefault("")
      }

    return buildString {
      appendLine(BFM_M3U.trim())
      rawPlaylists.forEach { raw ->
        if (raw.isNotBlank()) {
          appendLine(raw.trim().removePrefix("#EXTM3U").trim())
        }
      }
    }
  }

  val REMOTE_SOURCES =
    listOf(
      PlaylistPreset(
        id = "iptv_org_fr",
        label = "France (iptv-org)",
        countryTag = "FR",
        url = "https://iptv-org.github.io/iptv/countries/fr.m3u",
        forcedCountry = "France",
        forcedLanguage = "Français",
      ),
      PlaylistPreset(
        id = "freetv_fr",
        label = "France (Free-TV)",
        countryTag = "FR",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_france.m3u8",
        forcedCountry = "France",
        forcedLanguage = "Français",
      ),
      PlaylistPreset(
        id = "iptv_org_ca",
        label = "Canada (iptv-org)",
        countryTag = "CA",
        url = "https://iptv-org.github.io/iptv/countries/ca.m3u",
        forcedCountry = "Canada",
      ),
      PlaylistPreset(
        id = "freetv_be",
        label = "Belgique (Free-TV)",
        countryTag = "BE",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_belgium.m3u8",
        forcedCountry = "Belgique",
      ),
      PlaylistPreset(
        id = "iptv_org_afr",
        label = "Afrique (iptv-org)",
        countryTag = "AFR",
        url = "https://iptv-org.github.io/iptv/regions/afr.m3u",
        forcedCountry = "Afrique",
      ),
      PlaylistPreset(
        id = "iptv_org_uk",
        label = "Royaume-Uni (iptv-org)",
        countryTag = "UK",
        url = "https://iptv-org.github.io/iptv/countries/uk.m3u",
        forcedCountry = "Royaume-Uni",
        forcedLanguage = "Anglais",
      ),
      PlaylistPreset(
        id = "iptv_org_us",
        label = "États-Unis (iptv-org)",
        countryTag = "US",
        url = "https://iptv-org.github.io/iptv/countries/us.m3u",
        forcedCountry = "États-Unis",
        forcedLanguage = "Anglais",
      ),
      PlaylistPreset(
        id = "iptv_org_ng",
        label = "Nigeria (iptv-org)",
        countryTag = "NG",
        url = "https://iptv-org.github.io/iptv/countries/ng.m3u",
        forcedCountry = "Nigeria",
        forcedLanguage = "Anglais",
      ),
      PlaylistPreset(
        id = "iptv_org_za",
        label = "Afrique du Sud (iptv-org)",
        countryTag = "ZA",
        url = "https://iptv-org.github.io/iptv/countries/za.m3u",
        forcedCountry = "Afrique du Sud",
        forcedLanguage = "Anglais",
      ),
      PlaylistPreset(
        id = "cat_comedy",
        label = "Comédie (iptv-org)",
        countryTag = "COMEDY",
        url = "https://iptv-org.github.io/iptv/categories/comedy.m3u",
        forcedCategory = ChannelCategory.COMEDY,
      ),
      PlaylistPreset(
        id = "cat_movies",
        label = "Cinéma (iptv-org)",
        countryTag = "MOVIES",
        url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
        forcedCategory = ChannelCategory.MOVIES,
      ),
      PlaylistPreset(
        id = "cat_entertainment",
        label = "Divertissement (iptv-org)",
        countryTag = "ENT",
        url = "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
        forcedCategory = ChannelCategory.ENTERTAINMENT,
      ),
      PlaylistPreset(
        id = "cat_documentary",
        label = "Documentaires (iptv-org)",
        countryTag = "DOCS",
        url = "https://iptv-org.github.io/iptv/categories/documentary.m3u",
        forcedCategory = ChannelCategory.DOCUMENTARY,
      ),
      PlaylistPreset(
        id = "cat_kids",
        label = "Jeunesse (iptv-org)",
        countryTag = "KIDS",
        url = "https://iptv-org.github.io/iptv/categories/kids.m3u",
        forcedCategory = ChannelCategory.KIDS,
      ),
      PlaylistPreset(
        id = "cat_music",
        label = "Musique (iptv-org)",
        countryTag = "MUSIC",
        url = "https://iptv-org.github.io/iptv/categories/music.m3u",
        forcedCategory = ChannelCategory.MUSIC,
      ),
      PlaylistPreset(
        id = "cat_sport",
        label = "Sport (iptv-org)",
        countryTag = "SPORT",
        url = "https://iptv-org.github.io/iptv/categories/sport.m3u",
        forcedCategory = ChannelCategory.SPORTS,
      ),
      PlaylistPreset(
        id = "cat_news",
        label = "Actualités (iptv-org)",
        countryTag = "NEWS",
        url = "https://iptv-org.github.io/iptv/categories/news.m3u",
        forcedCategory = ChannelCategory.NEWS,
      ),
      PlaylistPreset(
        id = "cat_religious",
        label = "Religion (iptv-org)",
        countryTag = "REL",
        url = "https://iptv-org.github.io/iptv/categories/religious.m3u",
        forcedCategory = ChannelCategory.RELIGIOUS,
      ),
      PlaylistPreset(
        id = "iptvmate_sport_5",
        label = "Sport HD (iptvmate)",
        countryTag = "SPORT HD",
        url = "https://iptvmate.net/files/20012021/sport/Sport_5.iptvmate.net.m3u8",
        forcedCategory = ChannelCategory.SPORTS,
      ),
      PlaylistPreset(
        id = "iptvmate_sport_4",
        label = "Sport International",
        countryTag = "SPORT INT",
        url = "https://iptvmate.net/files/20012021/sport/Sport_4.iptvmate.net.m3u8",
        forcedCategory = ChannelCategory.SPORTS,
      ),
      PlaylistPreset(
        id = "iptvmate_sport_1",
        label = "Sport Premium",
        countryTag = "SPORT VIP",
        url = "https://iptvmate.net/files/20012021/sport/Sport_1.iptvmate.net.m3u8",
        forcedCategory = ChannelCategory.SPORTS,
      ),
      PlaylistPreset(
        id = "adult_18",
        label = "Adultes (+18)",
        countryTag = "+18",
        url = "https://pastebin.com/raw/jMe87UHE",
        forcedCategory = ChannelCategory.ADULT,
      ),
      PlaylistPreset(
        id = "freetv_global",
        label = "Free-TV Monde (Pays)",
        countryTag = "GLOBAL",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8",
        isCountryGrouped = true,
      ),
      PlaylistPreset(
        id = "iptv_org_by_language",
        label = "Monde par Langue (iptv-org)",
        countryTag = "LANG",
        url = "https://iptv-org.github.io/iptv/index.language.m3u",
        isLanguageGrouped = true,
      ),
      PlaylistPreset(
        id = "iptv_org_world",
        label = "Monde complet (iptv-org)",
        countryTag = "ALL",
        url = "https://iptv-org.github.io/iptv/index.m3u",
      ),
    )

  val EXTRA_PRESETS =
    listOf(
      PlaylistPreset(
        id = "iptv_org_by_country",
        label = "Monde par Pays (iptv-org)",
        countryTag = "PAYS",
        url = "https://iptv-org.github.io/iptv/index.country.m3u",
        isCountryGrouped = true,
      ),
      PlaylistPreset(
        id = "lang_fra",
        label = "Langue : Français",
        countryTag = "FR+",
        url = "https://iptv-org.github.io/iptv/languages/fra.m3u",
        forcedLanguage = "Français",
      ),
      PlaylistPreset(
        id = "lang_ara",
        label = "Langue : Arabe",
        countryTag = "ARA",
        url = "https://iptv-org.github.io/iptv/languages/ara.m3u",
        forcedLanguage = "Arabe",
      ),
      PlaylistPreset(
        id = "lang_spa",
        label = "Langue : Espagnol",
        countryTag = "SPA",
        url = "https://iptv-org.github.io/iptv/languages/spa.m3u",
        forcedLanguage = "Espagnol",
      ),
      PlaylistPreset(
        id = "lang_por",
        label = "Langue : Portugais",
        countryTag = "POR",
        url = "https://iptv-org.github.io/iptv/languages/por.m3u",
        forcedLanguage = "Portugais",
      ),
      PlaylistPreset(
        id = "lang_hin",
        label = "Langue : Hindi",
        countryTag = "HIN",
        url = "https://iptv-org.github.io/iptv/languages/hin.m3u",
        forcedLanguage = "Hindi",
      ),
      PlaylistPreset(
        id = "country_de",
        label = "Allemagne",
        countryTag = "DE",
        url = "https://iptv-org.github.io/iptv/countries/de.m3u",
        forcedCountry = "Allemagne",
        forcedLanguage = "Allemand",
      ),
      PlaylistPreset(
        id = "country_it",
        label = "Italie",
        countryTag = "IT",
        url = "https://iptv-org.github.io/iptv/countries/it.m3u",
        forcedCountry = "Italie",
        forcedLanguage = "Italien",
      ),
      PlaylistPreset(
        id = "country_es",
        label = "Espagne",
        countryTag = "ES",
        url = "https://iptv-org.github.io/iptv/countries/es.m3u",
        forcedCountry = "Espagne",
        forcedLanguage = "Espagnol",
      ),
      PlaylistPreset(
        id = "country_br",
        label = "Brésil",
        countryTag = "BR",
        url = "https://iptv-org.github.io/iptv/countries/br.m3u",
        forcedCountry = "Brésil",
        forcedLanguage = "Portugais",
      ),
      PlaylistPreset(
        id = "pastebin_intl",
        label = "International (Pastebin)",
        countryTag = "INTL",
        url = "https://pastebin.com/raw/hfRRAcpy",
      ),
      PlaylistPreset(
        id = "pastebin_us_ca",
        label = "USA & Canada",
        countryTag = "US/CA",
        url = "https://pastebin.com/raw/4cQ9mj8X",
      ),
      PlaylistPreset(
        id = "pastebin_rel",
        label = "Religion (Pastebin)",
        countryTag = "REL",
        url = "https://pastebin.com/raw/ikshwe1W",
        forcedCategory = ChannelCategory.RELIGIOUS,
      ),
    )

  val ALL_PRESETS = REMOTE_SOURCES + EXTRA_PRESETS
}
