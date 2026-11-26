package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultPlaylists
import com.example.data.M3uParser
import com.example.model.ChannelCategory
import org.junit.Assert.assertEquals
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
  fun parseFullBundledPlaylist_includesBfmAndAll328FrenchChannels() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fullM3u = DefaultPlaylists.loadBundledPlaylistM3u(context)
    val channels = M3uParser.parse(fullM3u)

    assertEquals(343, channels.size)
    assertEquals(343, channels.map { it.id }.toSet().size)

    val babyTv = channels.first { it.displayTitle == "FR: Baby TV" }
    assertEquals(ChannelCategory.KIDS, babyTv.category)
    assertEquals("Kids", babyTv.groupTitle)
    assertEquals("BT", babyTv.monogram)

    val amazonPrime2 = channels.first { it.displayTitle == "FR: Amazon Prime 2" }
    assertEquals(ChannelCategory.SPORTS, amazonPrime2.category)
    assertNull(amazonPrime2.tvgLogo)

    val viaOccitanie = channels.last()
    assertEquals("FR: ViaOccitanie", viaOccitanie.displayTitle)
    assertEquals(ChannelCategory.GENERAL, viaOccitanie.category)
  }

  @Test
  fun remoteSources_contains8UniqueCountryPlaylists_andSourceTagPrefixesGroup() {
    assertEquals(8, DefaultPlaylists.REMOTE_SOURCES.size)
    assertEquals(8, DefaultPlaylists.REMOTE_SOURCES.map { it.url }.toSet().size)

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
