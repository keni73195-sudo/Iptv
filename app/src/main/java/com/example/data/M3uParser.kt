package com.example.data

import com.example.model.Channel
import com.example.model.ChannelCategory
import kotlin.math.abs

object M3uParser {
  private val attributeRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
  private val resolutionRegex = Regex("""\s*\((\d{3,4}p|4K|UHD|HD|SD)\)\s*$""", RegexOption.IGNORE_CASE)
  private val tvgCountrySuffixRegex = Regex("""\.([a-z]{2})(?:@|$)""", RegexOption.IGNORE_CASE)

  fun parse(
    m3uContent: String,
    sourceTag: String? = null,
    preset: PlaylistPreset? = null,
  ): List<Channel> {
    if (m3uContent.isBlank()) return emptyList()

    val effectiveTag = preset?.countryTag ?: sourceTag
    val channels = mutableListOf<Channel>()
    val seenUrls = HashSet<String>()
    var pendingTvgId = ""
    var pendingLogo: String? = null
    var pendingGroup: String? = null
    var pendingTitle: String? = null
    var pendingUserAgent: String? = null
    var pendingReferrer: String? = null
    var pendingCountryAttr: String? = null
    var pendingLanguageAttr: String? = null
    var pendingCategoryAttr: String? = null

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
          pendingGroup = sanitizeGroupTitle(attrs["group-title"], effectiveTag)
          pendingTitle = titlePart.ifBlank { attrs["tvg-name"].orEmpty() }
          pendingCountryAttr = attrs["tvg-country"]?.takeIf { it.isNotBlank() }
          pendingLanguageAttr = attrs["tvg-language"]?.takeIf { it.isNotBlank() }
          pendingCategoryAttr = attrs["x-category"]?.takeIf { it.isNotBlank() }
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
        line.startsWith("#") -> Unit
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
            pendingCountryAttr = null
            pendingLanguageAttr = null
            pendingCategoryAttr = null
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

          val persistedCat =
            pendingCategoryAttr?.let { name ->
              ChannelCategory.entries.firstOrNull { it.name == name }
            }
          val category =
            preset?.forcedCategory
              ?: persistedCat
              ?: classifyCategory(cleanTitle, pendingGroup)

          val country =
            inferCountry(
              title = cleanTitle,
              tvgId = pendingTvgId,
              groupTitle = pendingGroup,
              attrCountry = pendingCountryAttr,
              preset = preset,
            )

          val language =
            inferLanguage(
              title = cleanTitle,
              groupTitle = pendingGroup,
              attrLanguage = pendingLanguageAttr,
              country = country,
              preset = preset,
            )

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
              country = country,
              language = language,
            )
          )

          pendingTvgId = ""
          pendingLogo = null
          pendingGroup = null
          pendingTitle = null
          pendingUserAgent = null
          pendingReferrer = null
          pendingCountryAttr = null
          pendingLanguageAttr = null
          pendingCategoryAttr = null
        }
      }
    }

    return channels
  }

  fun mergeEnrichedChannels(existing: List<Channel>, incoming: List<Channel>): List<Channel> {
    val byUrl = LinkedHashMap<String, Channel>(existing.size + incoming.size)
    existing.forEach { ch -> byUrl[ch.streamUrl] = ch }

    for (inc in incoming) {
      val cur = byUrl[inc.streamUrl]
      if (cur == null) {
        byUrl[inc.streamUrl] = inc
      } else {
        val mergedCategory =
          when {
            cur.category == ChannelCategory.ADULT || inc.category == ChannelCategory.ADULT ->
              ChannelCategory.ADULT
            cur.category != ChannelCategory.GENERAL -> cur.category
            else -> inc.category
          }
        val mergedCountry =
          if (cur.country != "International") cur.country else inc.country
        val mergedLanguage =
          if (cur.language != "Autre") cur.language else inc.language
        val mergedLogo = cur.tvgLogo ?: inc.tvgLogo
        val mergedGroup = cur.groupTitle ?: inc.groupTitle
        byUrl[inc.streamUrl] =
          cur.copy(
            category = mergedCategory,
            country = mergedCountry,
            language = mergedLanguage,
            tvgLogo = mergedLogo,
            groupTitle = mergedGroup,
          )
      }
    }

    return byUrl.values.mapIndexed { index, ch -> ch.copy(number = index + 1) }
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

  private fun inferCountry(
    title: String,
    tvgId: String,
    groupTitle: String?,
    attrCountry: String?,
    preset: PlaylistPreset?,
  ): String {
    if (!attrCountry.isNullOrBlank() && attrCountry != "International") return attrCountry
    if (preset?.forcedCountry != null) return preset.forcedCountry

    val rawGroupPart =
      groupTitle?.substringAfter("•")?.trim()?.takeIf { it.isNotBlank() } ?: groupTitle.orEmpty()

    if (preset?.isCountryGrouped == true && rawGroupPart.isNotBlank()) {
      mapCountryLabel(rawGroupPart)?.let { return it }
    }

    val suffix = tvgCountrySuffixRegex.find(tvgId)?.groupValues?.get(1)?.lowercase()
    countryFromIso(suffix)?.let { return it }

    val lowerTitle = title.lowercase()
    when {
      lowerTitle.startsWith("fr:") || lowerTitle.startsWith("fr ") || lowerTitle.contains("france") || lowerTitle.startsWith("bfm") ->
        return "France"
      lowerTitle.startsWith("be:") || lowerTitle.startsWith("be ") -> return "Belgique"
      lowerTitle.startsWith("ca:") || lowerTitle.startsWith("ca ") -> return "Canada"
      lowerTitle.startsWith("uk:") || lowerTitle.startsWith("uk ") -> return "Royaume-Uni"
      lowerTitle.startsWith("us:") || lowerTitle.startsWith("usa:") -> return "États-Unis"
    }

    if (rawGroupPart.isNotBlank()) {
      mapCountryLabel(rawGroupPart)?.let { return it }
    }

    return "International"
  }

  private fun inferLanguage(
    title: String,
    groupTitle: String?,
    attrLanguage: String?,
    country: String,
    preset: PlaylistPreset?,
  ): String {
    if (!attrLanguage.isNullOrBlank() && attrLanguage != "Autre") return attrLanguage
    if (preset?.forcedLanguage != null) return preset.forcedLanguage

    val rawGroupPart =
      groupTitle?.substringAfter("•")?.trim()?.takeIf { it.isNotBlank() } ?: groupTitle.orEmpty()

    if (preset?.isLanguageGrouped == true && rawGroupPart.isNotBlank()) {
      mapLanguageLabel(rawGroupPart)?.let { return it }
    }

    if (rawGroupPart.isNotBlank()) {
      mapLanguageLabel(rawGroupPart)?.let { return it }
    }

    val lowerTitle = title.lowercase()
    if (lowerTitle.contains("french") || lowerTitle.contains("français") || lowerTitle.startsWith("fr:")) {
      return "Français"
    }

    return when (country) {
      "France" -> "Français"
      "Royaume-Uni", "États-Unis", "Nigeria", "Afrique du Sud", "Australie", "Irlande" -> "Anglais"
      "Espagne", "Mexique", "Argentine", "Colombie" -> "Espagnol"
      "Portugal", "Brésil" -> "Portugais"
      "Allemagne", "Autriche" -> "Allemand"
      "Italie" -> "Italien"
      "Arabie Saoudite", "Égypte", "Émirats Arabes", "Qatar" -> "Arabe"
      "Maroc", "Algérie", "Tunisie" -> "Arabe"
      "Turquie" -> "Turc"
      "Russie" -> "Russe"
      "Inde" -> "Hindi"
      "Chine", "Hong Kong", "Taïwan" -> "Chinois"
      "Japon" -> "Japonais"
      "Corée du Sud" -> "Coréen"
      "Pays-Bas" -> "Néerlandais"
      "Grèce" -> "Grec"
      "Pologne" -> "Polonais"
      else -> "Autre"
    }
  }

  private fun countryFromIso(code: String?): String? =
    when (code) {
      "fr" -> "France"
      "be" -> "Belgique"
      "ca" -> "Canada"
      "ch" -> "Suisse"
      "uk", "gb" -> "Royaume-Uni"
      "us" -> "États-Unis"
      "de" -> "Allemagne"
      "es" -> "Espagne"
      "it" -> "Italie"
      "pt" -> "Portugal"
      "br" -> "Brésil"
      "nl" -> "Pays-Bas"
      "gr" -> "Grèce"
      "ma" -> "Maroc"
      "dz" -> "Algérie"
      "tn" -> "Tunisie"
      "sn" -> "Sénégal"
      "ci" -> "Côte d'Ivoire"
      "cm" -> "Cameroun"
      "cd", "cg" -> "Congo"
      "ng" -> "Nigeria"
      "za" -> "Afrique du Sud"
      "eg" -> "Égypte"
      "sa" -> "Arabie Saoudite"
      "ae" -> "Émirats Arabes"
      "qa" -> "Qatar"
      "tr" -> "Turquie"
      "ru" -> "Russie"
      "in" -> "Inde"
      "cn" -> "Chine"
      "hk" -> "Hong Kong"
      "jp" -> "Japon"
      "kr" -> "Corée du Sud"
      "mx" -> "Mexique"
      "ar" -> "Argentine"
      "au" -> "Australie"
      "id" -> "Indonésie"
      "pl" -> "Pologne"
      "ro" -> "Roumanie"
      else -> null
    }

  private fun mapCountryLabel(raw: String): String? {
    val firstToken = raw.split(";").firstOrNull()?.trim()?.lowercase().orEmpty()
    return when {
      firstToken == "france" || firstToken == "fr" -> "France"
      firstToken == "canada" || firstToken == "ca" -> "Canada"
      firstToken == "belgium" || firstToken == "belgique" || firstToken == "be" -> "Belgique"
      firstToken == "switzerland" || firstToken == "suisse" || firstToken == "ch" -> "Suisse"
      firstToken == "united kingdom" || firstToken == "uk" || firstToken == "great britain" -> "Royaume-Uni"
      firstToken == "united states" || firstToken == "usa" || firstToken == "us" -> "États-Unis"
      firstToken == "germany" || firstToken == "allemagne" || firstToken == "de" -> "Allemagne"
      firstToken == "spain" || firstToken == "espagne" || firstToken == "es" -> "Espagne"
      firstToken == "italy" || firstToken == "italie" || firstToken == "it" -> "Italie"
      firstToken == "portugal" || firstToken == "pt" -> "Portugal"
      firstToken == "brazil" || firstToken == "brésil" || firstToken == "br" -> "Brésil"
      firstToken == "netherlands" || firstToken == "pays-bas" || firstToken == "nl" -> "Pays-Bas"
      firstToken == "greece" || firstToken == "grèce" || firstToken == "gr" -> "Grèce"
      firstToken == "morocco" || firstToken == "maroc" || firstToken == "ma" -> "Maroc"
      firstToken == "algeria" || firstToken == "algérie" || firstToken == "dz" -> "Algérie"
      firstToken == "tunisia" || firstToken == "tunisie" || firstToken == "tn" -> "Tunisie"
      firstToken == "senegal" || firstToken == "sénégal" || firstToken == "sn" -> "Sénégal"
      firstToken == "ivory coast" || firstToken == "côte d'ivoire" -> "Côte d'Ivoire"
      firstToken == "cameroon" || firstToken == "cameroun" -> "Cameroun"
      firstToken == "nigeria" || firstToken == "ng" -> "Nigeria"
      firstToken == "south africa" || firstToken == "afrique du sud" || firstToken == "za" -> "Afrique du Sud"
      firstToken == "africa" || firstToken == "afrique" || firstToken == "afr" -> "Afrique"
      firstToken == "egypt" || firstToken == "égypte" -> "Égypte"
      firstToken == "saudi arabia" -> "Arabie Saoudite"
      firstToken == "turkey" || firstToken == "turquie" -> "Turquie"
      firstToken == "russia" || firstToken == "russie" || firstToken == "ru" -> "Russie"
      firstToken == "india" || firstToken == "inde" -> "Inde"
      firstToken == "china" || firstToken == "chine" || firstToken == "cn" -> "Chine"
      firstToken == "hong kong" || firstToken == "hk" -> "Hong Kong"
      firstToken == "japan" || firstToken == "japon" || firstToken == "jp" -> "Japon"
      firstToken == "south korea" || firstToken == "korea" || firstToken == "kr" -> "Corée du Sud"
      firstToken == "indonesia" || firstToken == "indonésie" -> "Indonésie"
      firstToken == "mexico" || firstToken == "mexique" -> "Mexique"
      firstToken == "argentina" || firstToken == "argentine" -> "Argentine"
      firstToken == "australia" || firstToken == "australie" -> "Australie"
      firstToken == "poland" || firstToken == "pologne" -> "Pologne"
      firstToken == "romania" || firstToken == "roumanie" -> "Roumanie"
      else -> null
    }
  }

  private fun mapLanguageLabel(raw: String): String? {
    val firstToken = raw.split(";").firstOrNull()?.trim()?.lowercase().orEmpty()
    return when (firstToken) {
      "french", "français", "francais", "fra" -> "Français"
      "english", "anglais", "eng" -> "Anglais"
      "arabic", "arabe", "ara" -> "Arabe"
      "spanish", "espagnol", "spa" -> "Espagnol"
      "portuguese", "portugais", "por" -> "Portugais"
      "german", "allemand", "deu", "ger" -> "Allemand"
      "italian", "italien", "ita" -> "Italien"
      "hindi", "hin", "telugu" -> "Hindi"
      "turkish", "turc", "tur" -> "Turc"
      "russian", "russe", "rus" -> "Russe"
      "chinese", "chinois", "zho", "chi", "mandarin", "cantonese" -> "Chinois"
      "japanese", "japonais", "jpn" -> "Japonais"
      "korean", "coréen", "kor" -> "Coréen"
      "dutch", "néerlandais", "nld" -> "Néerlandais"
      "greek", "grec", "ell" -> "Grec"
      "polish", "polonais", "pol" -> "Polonais"
      "romanian", "roumain", "ron" -> "Roumain"
      "indonesian", "indonésien", "ind" -> "Indonésien"
      else -> null
    }
  }

  private fun classifyCategory(title: String, groupTitle: String?): ChannelCategory {
    val lowerTitle = title.lowercase()
    val lowerGroup = groupTitle?.lowercase().orEmpty()

    return when {
      lowerGroup.contains("+18") ||
        lowerGroup.contains("adult") ||
        lowerGroup.contains("xxx") ||
        lowerTitle.contains("xxx") ||
        lowerTitle.contains("18+") ||
        lowerTitle.contains("adult") ||
        lowerTitle.contains("erotic") ||
        lowerTitle.contains("porn") ||
        lowerTitle.contains("playboy") ||
        lowerTitle.contains("penthouse") ||
        lowerTitle.contains("dorcel") ||
        lowerTitle.contains("brazzers") ||
        lowerTitle.contains("venus") -> ChannelCategory.ADULT
      lowerGroup.contains("kid") ||
        lowerGroup.contains("jeunesse") ||
        lowerGroup.contains("animation") ||
        lowerTitle.contains("cartoon") ||
        lowerTitle.contains("disney") ||
        lowerTitle.contains("gulli") ||
        lowerTitle.contains("nickelodeon") -> ChannelCategory.KIDS
      lowerGroup.contains("comedy") ||
        lowerGroup.contains("comédie") ||
        lowerGroup.contains("humour") ||
        lowerTitle.contains("comedy") -> ChannelCategory.COMEDY
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
        lowerTitle.contains("cine+") ||
        lowerTitle.contains("ciné") -> ChannelCategory.MOVIES
      lowerGroup.contains("entertain") ||
        lowerGroup.contains("divertissement") ||
        lowerGroup.contains("lifestyle") -> ChannelCategory.ENTERTAINMENT
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
      lowerGroup.contains("religio") ||
        lowerGroup.contains("religion") ||
        lowerTitle.contains("kto") ||
        lowerTitle.contains("emci") ||
        lowerTitle.contains("quran") ||
        lowerTitle.contains("gospel") -> ChannelCategory.RELIGIOUS
      lowerGroup.contains("news") ||
        lowerGroup.contains("info") ||
        lowerGroup.contains("actualit") ||
        lowerTitle.startsWith("bfm") ||
        lowerTitle.contains("cnews") ||
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
