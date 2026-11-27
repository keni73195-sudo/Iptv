package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultPlaylists
import com.example.data.M3uParser
import com.example.model.ChannelCategory
import com.example.ui.IptvViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3uParserTest {

  @Test
  fun parseDefaultBfmPlaylist_returnsAll15ChannelsWithUserAgentAndStreamUrls() {
    val channels = M3uParser.parse(DefaultPlaylists.BFM_M3U)

    assertEquals(15, channels.size)

    val expectedUserAgent =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
    assertTrue(channels.all { it.userAgent == expectedUserAgent })
    assertTrue(channels.all { it.streamUrl.startsWith("https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE$") })
    assertTrue(channels.all { it.resolution == "1080P" })

    val bfm2 = channels.first()
    assertEquals("BFM2.fr@HD", bfm2.tvgId)
    assertEquals("BFM 2", bfm2.displayTitle)
    assertEquals(ChannelCategory.NEWS, bfm2.category)

    val grandsReportages = channels[7]
    assertEquals("", grandsReportages.tvgId)
    assertEquals("BFM Grands Reportages", grandsReportages.displayTitle)
    assertTrue(grandsReportages.id.isNotBlank())

    val bfmTv = channels.last()
    assertEquals("BFMTV.fr@SD", bfmTv.tvgId)
    assertEquals("BFM TV", bfmTv.displayTitle)
  }

  @Test
  fun parseFullBundledPlaylist_includesBfmAndBothFrenchPlaylistsDeduplicated() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fullM3u = DefaultPlaylists.loadBundledPlaylistM3u(context)
    val channels = M3uParser.parse(fullM3u)

    assertTrue("Expected > 600 channels, was ${channels.size}", channels.size > 600)
    assertEquals(channels.size, channels.map { it.id }.toSet().size)
    assertEquals(channels.size, channels.map { it.streamUrl }.toSet().size)
    assertTrue(channels.none { it.streamUrl.contains("#origine") })

    val babyTv = channels.first { it.displayTitle == "FR: Baby TV" }
    assertEquals(ChannelCategory.KIDS, babyTv.category)
    assertEquals("Kids", babyTv.groupTitle)
    assertEquals("BT", babyTv.monogram)

    val amazonPrime2 = channels.first { it.displayTitle == "FR: Amazon Prime 2" }
    assertEquals(ChannelCategory.SPORTS, amazonPrime2.category)
    assertNull(amazonPrime2.tvgLogo)

    assertTrue(channels.any { it.displayTitle == "CANAL ALGERIE" && it.category == ChannelCategory.NEWS })
    assertTrue(channels.any { it.displayTitle == "PARIS PREMIERE" && it.streamUrl.contains(".mpd") })
    assertTrue(channels.any { it.displayTitle == "NOVO 19" && it.streamUrl.contains(".mpd") })
    assertTrue(channels.any { it.displayTitle == "ESPN 1 HD" && it.groupTitle == "USA" })
  }

  @Test
  fun parseExtInfWithCommaAfterDuration_extractsAttributesCategoriesLogosAndDeduplicatesUrls() {
    val sample = """
      #EXTM3U
      #EXTINF:-1, tvg-id="CanalAlgerie.ar" tvg-logo="Logo N/A" tvg-name="CANAL ALGERIE"  group-title="ACTUALITES",CANAL ALGERIE
      https://example.com/canal-algerie.m3u8
      #EXTINF:-1, tvg-id="CanalAlgerie.ar" tvg-logo="Logo N/A" tvg-name="CANAL ALGERIE"  group-title="ACTUALITES",CANAL ALGERIE
      https://example.com/canal-algerie.m3u8#origine-101=https://tvradiozap.eu
      #EXTINF:-1, tvg-id="VevoPop.fr" tvg-logo="https://example.com/vevo.png" tvg-name="VEVO Pop"  group-title="MUSIQUE",VEVO Pop
      https://example.com/vevo-pop.m3u8#origine-863=https://tvradiozap.eu
      #EXTINF:-1, tvg-id="Tennis.us" tvg-logo="New Logo" tvg-name="TENNIS HD"  group-title="USA",TENNIS HD
      https://example.com/tennis.m3u8
    """.trimIndent()

    val parsed = M3uParser.parse(sample)
    assertEquals(3, parsed.size)

    val canalAlgerie = parsed[0]
    assertEquals("CANAL ALGERIE", canalAlgerie.displayTitle)
    assertEquals("CanalAlgerie.ar", canalAlgerie.tvgId)
    assertEquals("ACTUALITES", canalAlgerie.groupTitle)
    assertEquals(ChannelCategory.NEWS, canalAlgerie.category)
    assertEquals("https://example.com/canal-algerie.m3u8", canalAlgerie.streamUrl)
    assertNull(canalAlgerie.tvgLogo)

    val vevoPop = parsed[1]
    assertEquals("VEVO Pop", vevoPop.displayTitle)
    assertEquals(ChannelCategory.MUSIC, vevoPop.category)
    assertEquals("https://example.com/vevo.png", vevoPop.tvgLogo)
    assertEquals("https://example.com/vevo-pop.m3u8", vevoPop.streamUrl)

    val tennis = parsed[2]
    assertEquals("TENNIS HD", tennis.displayTitle)
    assertNull(tennis.tvgLogo)
  }

  @Test
  fun findBackupChannel_selectsAlternativeWorkingStreamForSameChannel() {
    val sample = """
      #EXTM3U
      #EXTINF:-1 tvg-id="TMC.fr" group-title="GENERALISTE",TMC FHD
      http://dead-xtream.example/116087
      #EXTINF:-1 tvg-id="TMC.fr" group-title="GENERALISTE",TMC
      https://backup.example/tmc.m3u8
    """.trimIndent()
    val channels = M3uParser.parse(sample)
    val backup = IptvViewModel.findBackupChannel(channels, channels[0], setOf(channels[0].id))
    assertNotNull(backup)
    assertEquals("https://backup.example/tmc.m3u8", backup?.streamUrl)
  }

  @Test
  fun remoteSources_contains10UniquePlaylists_andSourceTagPrefixesGroup() {
    assertEquals(10, DefaultPlaylists.REMOTE_SOURCES.size)
    assertEquals(10, DefaultPlaylists.REMOTE_SOURCES.map { it.url }.toSet().size)
    assertTrue(
      DefaultPlaylists.REMOTE_SOURCES.any {
        it.url == "https://iptv-org.github.io/iptv/regions/afr.m3u" && it.countryTag == "AFR"
      }
    )
    assertTrue(
      DefaultPlaylists.REMOTE_SOURCES.any {
        it.url == "https://iptv-org.github.io/iptv/countries/za.m3u" && it.countryTag == "ZA"
      }
    )

    val sampleFreeTv = """
      #EXTM3U
      #EXTINF:-1 tvg-id="LaUne.be" group-title="Belgium",La Une
      https://example.com/laune.m3u8
    """.trimIndent()
    val parsed = M3uParser.parse(sampleFreeTv, sourceTag = "BE")
    assertEquals(1, parsed.size)
    assertEquals("BE • Belgium", parsed.first().groupTitle)
  }
}
