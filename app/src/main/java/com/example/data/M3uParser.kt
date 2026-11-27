package com.example.data

import com.example.model.Channel
import com.example.model.ChannelCategory
import kotlin.math.abs

object M3uParser {
  private val attributeRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
  private val resolutionRegex = Regex("""\s*\((\d{3,4}p|4K|UHD|HD|SD)\)\s*$""", RegexOption.IGNORE_CASE)

  fun parse(m3uContent: String, sourceTag: String? = null): List<Channel> {
    if (m3uContent.isBlank()) return emptyList()

    val channels = mutableListOf<Channel>()
    val seenUrls = HashSet<String>()
    var pendingTvgId = ""
    var pendingLogo: String? = null
    var pendingGroup: String? = null
    var pendingTitle: String? = null
    var pendingUserAgent: String? = null
    var pendingReferrer: String? = null

    val lines = m3uContent.lineSequence().map { it.trim() }
    for (line in lines) {
      if (line.isEmpty() || line.startsWith("#EXTM3U")) continue

      when {
        line.startsWith("#EXTINF:") -> {
          pendingUserAgent = null
          pendingReferrer = null

          val commaIndex = findMetadataSeparatorIndex(line)
          val metadataPart = if (commaIndex >= 0) line.substring(0, commaIndex) else line
          val titlePart = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else ""

          val attrs = mutableMapOf<String, String>()
          attributeRegex.findAll(metadataPart).forEach { match ->
            attrs[match.groupValues[1].lowercase()] = match.groupValues[2]
          }

          pendingTvgId = attrs["tvg-id"].orEmpty()
          pendingLogo = sanitizeLogoUrl(attrs["tvg-logo"])
          pendingGroup = sanitizeGroupTitle(attrs["group-title"], sourceTag)
          pendingTitle = titlePart.ifBlank { attrs["tvg-name"].orEmpty() }
        }
        line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
          val option = line.substringAfter(":", "").trim()
          when {
            option.startsWith("http-user-agent=", ignoreCase = true) -> {
              pendingUserAgent = option.substringAfter("=", "").trim().takeIf { it.isNotEmpty() }
            }
            option.startsWith("http-referrer=", ignoreCase = true) ||
              option.startsWith("http-referer=", ignoreCase = true) -> {
              pendingReferrer = option.substringAfter("=", "").trim().takeIf { it.isNotEmpty() }
            }
          }
        }
        line.startsWith("#") -> {
          // Ignore other comment directives (#EXTGRP, #KODIPROP, etc.)
        }
        line.startsWith("http://", ignoreCase = true) ||
          line.startsWith("https://", ignoreCase = true) -> {
          val cleanUrl = sanitizeStreamUrl(line)
          if (!seenUrls.add(cleanUrl)) {
            pendingTvgId = ""
            pendingLogo = null
            pendingGroup = null
            pendingTitle = null
            pendingUserAgent = null
            pendingReferrer = null
            continue
          }
          val rawName = pendingTitle?.takeIf { it.isNotBlank() } ?: "Flux ${channels.size + 1}"
          val resMatch = resolutionRegex.find(rawName)
          val resolution = resMatch?.groupValues?.get(1)?.uppercase()
          val cleanTitle =
            if (resMatch != null) rawName.removeRange(resMatch.range).trim() else rawName

          val baseId =
            pendingTvgId.takeIf { it.isNotBlank() }
              ?: cleanTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_")
          val stableId = "${baseId}_${abs(cleanUrl.hashCode())}"

          val category = classifyCategory(cleanTitle, pendingGroup)
          channels.add(
            Channel(
              id = stableId,
              number = channels.size + 1,
              name = rawName,
              displayTitle = cleanTitle,
              resolution = resolution,
              tvgId = pendingTvgId,
              tvgLogo = pendingLogo,
              groupTitle = pendingGroup,
              userAgent = pendingUserAgent,
              referrer = pendingReferrer,
              streamUrl = cleanUrl,
              category = category,
              monogram = buildMonogram(cleanTitle),
              accentIndex = abs(stableId.hashCode()) % 6,
            )
          )

          pendingTvgId = ""
          pendingLogo = null
          pendingGroup = null
          pendingTitle = null
          pendingUserAgent = null
          pendingReferrer = null
        }
      }
    }

    return channels
  }

  private fun sanitizeStreamUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    val origineIdx = trimmed.indexOf("#origine", ignoreCase = true)
    return if (origineIdx > 0) trimmed.substring(0, origineIdx).trim() else trimmed
  }

  private fun sanitizeLogoUrl(rawLogo: String?): String? {
    val trimmed = rawLogo?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (!trimmed.startsWith("http://", ignoreCase = true) &&
      !trimmed.startsWith("https://", ignoreCase = true)
    ) {
      return null
    }
    if (trimmed.endsWith("blank-icon.png", ignoreCase = true)) return null
    if (trimmed.endsWith(".svg", ignoreCase = true)) return null
    return trimmed
  }

  private fun sanitizeGroupTitle(rawGroup: String?, sourceTag: String?): String? {
    val cleaned =
      rawGroup
        ?.trim()
        ?.replace("France ➾ ", "")
        ?.replace("➾", "•")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    return when {
      sourceTag != null && cleaned != null && !cleaned.startsWith(sourceTag) -> "$sourceTag • $cleaned"
      cleaned != null -> cleaned
      else -> sourceTag
    }
  }

  private fun findMetadataSeparatorIndex(extInfLine: String): Int {
    val lastQuote = extInfLine.lastIndexOf('"')
    if (lastQuote >= 0) {
      val commaAfterAttrs = extInfLine.indexOf(',', startIndex = lastQuote + 1)
      if (commaAfterAttrs >= 0) return commaAfterAttrs
    }
    return extInfLine.indexOf(',')
  }

  private fun classifyCategory(title: String, groupTitle: String?): ChannelCategory {
    val lowerTitle = title.lowercase()
    val lowerGroup = groupTitle?.lowercase().orEmpty()

    return when {
      lowerGroup.contains("kid") ||
        lowerGroup.contains("jeunesse") ||
        lowerGroup.contains("animation") ||
        lowerTitle.contains("cartoon") ||
        lowerTitle.contains("disney") ||
        lowerTitle.contains("nickelodeon") -> ChannelCategory.KIDS
      lowerGroup.contains("foot") ||
        lowerGroup.contains("sport") ||
        lowerTitle.contains("bein") ||
        lowerTitle.contains("eurosport") ||
        lowerTitle.contains("rmc sport") ||
        lowerTitle.contains("dazn") ||
        lowerTitle.contains("espn") ||
        lowerTitle.contains("sky sports") -> ChannelCategory.SPORTS
      lowerGroup.contains("movie") ||
        lowerGroup.contains("cinema") ||
        lowerGroup.contains("ciné") ||
        lowerGroup.contains("series") ||
        lowerTitle.contains("cine+") -> ChannelCategory.MOVIES
      lowerGroup.contains("doc") ||
        lowerGroup.contains("nature") ||
        lowerGroup.contains("science") ||
        lowerTitle.contains("planete+") ||
        lowerTitle.contains("discovery") ||
        lowerTitle.contains("nat geo") -> ChannelCategory.DOCUMENTARY
      lowerGroup.contains("music") ||
        lowerGroup.contains("musique") ||
        lowerTitle.contains("mtv") ||
        lowerTitle.contains("trace ") ||
        lowerTitle.contains("nrj hits") -> ChannelCategory.MUSIC
      lowerGroup.contains("news") ||
        lowerGroup.contains("info") ||
        lowerGroup.contains("actualit") ||
        lowerTitle.startsWith("bfm") ||
        lowerTitle.contains("c news") ||
        lowerTitle.contains("france 24") ||
        lowerTitle.contains("euronews") ||
        lowerTitle.contains("cnn") ||
        lowerTitle.contains("bbc news") ||
        lowerTitle.contains("sky news") ||
        lowerTitle.contains("cbc news") ||
        lowerTitle.contains("lci") -> ChannelCategory.NEWS
      else -> ChannelCategory.GENERAL
    }
  }

  private fun buildMonogram(title: String): String {
    val withoutPrefix =
      title
        .replace(Regex("^FR\\s*[:\\-]\\s*", RegexOption.IGNORE_CASE), "")
        .replace("BFM", "", ignoreCase = true)
        .trim()

    val words =
      withoutPrefix
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() && it != "&" && it != "+" }

    return when {
      title.equals("BFM TV", ignoreCase = true) -> "TV"
      title.equals("BFM 2", ignoreCase = true) -> "B2"
      words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
      words.size == 1 -> words[0].take(2).uppercase()
      else -> title.take(2).uppercase()
    }
  }
}
