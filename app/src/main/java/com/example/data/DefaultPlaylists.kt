package com.example.data

import android.content.Context
import com.example.R

data class PlaylistPreset(
  val id: String,
  val label: String,
  val countryTag: String,
  val url: String,
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
    val extraM3u =
      runCatching {
        context.resources.openRawResource(R.raw.fr_channels_playlist).bufferedReader().use {
          it.readText()
        }
      }
        .getOrDefault("")

    return buildString {
      appendLine(BFM_M3U.trim())
      if (extraM3u.isNotBlank()) {
        appendLine(extraM3u.trim().removePrefix("#EXTM3U").trim())
      }
    }
  }

  val REMOTE_SOURCES =
    listOf(
      PlaylistPreset(
        id = "freetv_be",
        label = "Belgique (Free-TV)",
        countryTag = "BE",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_belgium.m3u8",
      ),
      PlaylistPreset(
        id = "freetv_ca",
        label = "Canada (Free-TV)",
        countryTag = "CA",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_canada.m3u8",
      ),
      PlaylistPreset(
        id = "iptv_org_fr",
        label = "France (iptv-org)",
        countryTag = "FR",
        url = "https://iptv-org.github.io/iptv/countries/fr.m3u",
      ),
      PlaylistPreset(
        id = "freetv_fr",
        label = "France (Free-TV)",
        countryTag = "FR",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_france.m3u8",
      ),
      PlaylistPreset(
        id = "iptv_org_ng",
        label = "Nigeria (iptv-org)",
        countryTag = "NG",
        url = "https://iptv-org.github.io/iptv/countries/ng.m3u",
      ),
      PlaylistPreset(
        id = "iptv_org_uk",
        label = "UK (iptv-org)",
        countryTag = "UK",
        url = "https://iptv-org.github.io/iptv/countries/uk.m3u",
      ),
      PlaylistPreset(
        id = "iptv_org_us",
        label = "USA (iptv-org)",
        countryTag = "US",
        url = "https://iptv-org.github.io/iptv/countries/us.m3u",
      ),
      PlaylistPreset(
        id = "freetv_us",
        label = "USA (Free-TV)",
        countryTag = "US",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_usa.m3u8",
      ),
    )

  val EXTRA_PRESETS =
    listOf(
      PlaylistPreset(
        id = "iptv_org_fra",
        label = "Francophone (iptv-org)",
        countryTag = "FR+",
        url = "https://iptv-org.github.io/iptv/languages/fra.m3u",
      ),
      PlaylistPreset(
        id = "iptv_org_world",
        label = "Monde (iptv-org)",
        countryTag = "ALL",
        url = "https://iptv-org.github.io/iptv/index.m3u",
      ),
    )

  val ALL_PRESETS = REMOTE_SOURCES + EXTRA_PRESETS
}
