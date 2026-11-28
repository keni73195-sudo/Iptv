package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

enum class SubscriptionPlan(
  val id: String,
  val label: String,
  val codePrefix: String,
  val durationMillis: Long,
  val badge: String,
) {
  TRIAL_10M(
    id = "TRIAL_10M",
    label = "Essai 10 min",
    codePrefix = "10M",
    durationMillis = 10L * 60_000L,
    badge = "10 MIN",
  ),
  ONE_MONTH(
    id = "ONE_MONTH",
    label = "1 Mois",
    codePrefix = "1M",
    durationMillis = 30L * 24L * 3_600_000L,
    badge = "1 MOIS",
  ),
  SIX_MONTHS(
    id = "SIX_MONTHS",
    label = "6 Mois",
    codePrefix = "6M",
    durationMillis = 180L * 24L * 3_600_000L,
    badge = "6 MOIS",
  ),
  ONE_YEAR(
    id = "ONE_YEAR",
    label = "1 An",
    codePrefix = "1A",
    durationMillis = 365L * 24L * 3_600_000L,
    badge = "1 AN",
  ),
}

enum class ActivationCodeStatus {
  AVAILABLE,
  REDEEMED,
  REVOKED;

  companion object {
    fun fromRaw(raw: String?): ActivationCodeStatus =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AVAILABLE
  }
}

@Keep
@IgnoreExtraProperties
data class ActivationCode(
  val code: String = "",
  val planLabel: String = "",
  val durationMillis: Long = 0L,
  val status: String = ActivationCodeStatus.AVAILABLE.name,
  val createdByUid: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
  val redeemedByUid: String = "",
  val redeemedProfileName: String = "",
  val redeemedDeviceId: String = "",
  val redeemedAt: Timestamp? = null,
  val expiresAtMillis: Long = 0L,
) {
  fun parsedStatus(): ActivationCodeStatus = ActivationCodeStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  val userId: String = "",
  val profileName: String = "",
  val email: String = "",
  val activeCode: String = "",
  val planLabel: String = "",
  val durationMillis: Long = 0L,
  val deviceId: String = "",
  val revoked: Boolean = false,
  val activatedAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
  val expiresAtMillis: Long = 0L,
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
  val uid: String = "",
  val email: String = "",
  val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class RemotePlaylistSource(
  val id: String = "",
  val label: String = "",
  val url: String = "",
  val countryTag: String = "LIVE",
  val forcedCategory: String = "",
  val forcedCountry: String = "",
  val forcedLanguage: String = "",
  val enabled: Boolean = true,
  val createdByUid: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toPlaylistPreset(): com.example.data.PlaylistPreset =
    com.example.data.PlaylistPreset(
      id = "remote_$id",
      label = label.ifBlank { "Source" },
      countryTag = countryTag.ifBlank { "LIVE" },
      url = url,
      forcedCategory = ChannelCategory.entries.firstOrNull { it.name == forcedCategory },
      forcedCountry = forcedCountry.takeIf { it.isNotBlank() },
      forcedLanguage = forcedLanguage.takeIf { it.isNotBlank() },
    )
}

sealed interface AccessGateState {
  data object Loading : AccessGateState

  data class NeedsEmailAuth(
    val isCreateAccount: Boolean = false,
    val isSubmitting: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
  ) : AccessGateState

  data class NeedsAdminVerification(
    val uid: String,
    val email: String,
    val isSending: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
  ) : AccessGateState

  data class NeedsActivation(
    val uid: String,
    val email: String,
    val suggestedName: String = "",
    val previousProfile: UserProfile? = null,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
  ) : AccessGateState

  data class Authorized(
    val uid: String,
    val email: String,
    val isAdmin: Boolean,
    val profile: UserProfile?,
    val remainingMillis: Long,
  ) : AccessGateState
}

fun formatRemainingDuration(remainingMillis: Long): String {
  if (remainingMillis <= 0L) return "Expiré"
  val totalSeconds = remainingMillis / 1000L
  val days = totalSeconds / 86_400L
  val hours = (totalSeconds % 86_400L) / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return when {
    days >= 1L -> "${days}j ${hours}h"
    hours >= 1L -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
    else -> "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
  }
}
