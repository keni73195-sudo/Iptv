package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AccessRepository
import com.example.model.ActivationCode
import com.example.model.ActivationCodeStatus
import com.example.model.ChannelCategory
import com.example.model.RemotePlaylistSource
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.model.formatRemainingDuration
import com.example.ui.components.AdminSourcesSection
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ControlSurface
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanBroadcast
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OrangeAccent

private enum class CodeFilterTab(val label: String) {
  ALL("Tous"),
  AVAILABLE("Disponibles"),
  ACTIVE("Actifs"),
  EXPIRED_OR_REVOKED("Expirés / Révoqués"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(
  codes: List<ActivationCode>,
  profiles: List<UserProfile>,
  sources: List<RemotePlaylistSource>,
  latestGeneratedCode: ActivationCode?,
  isBusy: Boolean,
  bannerMessage: String?,
  serverNowMillis: Long,
  onGenerateCode: (SubscriptionPlan, String?) -> Unit,
  onRevokeCode: (ActivationCode) -> Unit,
  onDeleteCode: (String) -> Unit,
  onToggleUserRevocation: (UserProfile) -> Unit,
  onExtendUserPlan: (UserProfile, SubscriptionPlan) -> Unit,
  onAddSource: (
    label: String,
    url: String,
    countryTag: String,
    forcedCategory: ChannelCategory?,
    forcedCountry: String,
    forcedLanguage: String,
  ) -> Unit,
  onToggleSource: (RemotePlaylistSource) -> Unit,
  onDeleteSource: (String) -> Unit,
  onSyncSourcesNow: () -> Unit,
  onOpenLocalImport: () -> Unit,
  onClearBanner: () -> Unit,
  onClose: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onClose() }

  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedSection by remember { mutableStateOf("CODES") }
  var codeFilter by remember { mutableStateOf(CodeFilterTab.ALL) }
  var customCodeInput by remember { mutableStateOf("") }

  LaunchedEffect(bannerMessage) {
    if (!bannerMessage.isNullOrBlank()) {
      snackbarHostState.showSnackbar(bannerMessage)
      onClearBanner()
    }
  }

  val filteredCodes =
    remember(codes, codeFilter, serverNowMillis) {
      codes.filter { code ->
        val status = code.parsedStatus()
        val isExpired =
          status == ActivationCodeStatus.REDEEMED &&
            code.expiresAtMillis > 0L &&
            code.expiresAtMillis <= serverNowMillis
        when (codeFilter) {
          CodeFilterTab.ALL -> true
          CodeFilterTab.AVAILABLE -> status == ActivationCodeStatus.AVAILABLE
          CodeFilterTab.ACTIVE -> status == ActivationCodeStatus.REDEEMED && !isExpired
          CodeFilterTab.EXPIRED_OR_REVOKED ->
            status == ActivationCodeStatus.REVOKED || isExpired
        }
      }
    }

  Scaffold(
    containerColor = ObsidianBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .windowInsetsPadding(WindowInsets.statusBars)
          .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
      Surface(
        color = ControlSurface,
        border = BorderStroke(1.dp, BorderSlate),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("admin_back_to_tv_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = stringResource(R.string.admin_back_to_tv),
                  tint = OrangeAccent,
                )
              }
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = CyanBroadcast,
                modifier = Modifier.size(20.dp),
              )
              Column {
                Text(
                  text = stringResource(R.string.admin_panel_title),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${codes.size} codes • ${profiles.size} abonnés • ${sources.size} sources",
                  style = MaterialTheme.typography.labelSmall,
                  color = InkSecondary,
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(
                onClick = onClose,
                border = BorderStroke(1.dp, OrangeAccent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(40.dp),
              ) {
                Text(
                  text = stringResource(R.string.admin_watch_tv),
                  color = OrangeAccent,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
              }
              IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("admin_sign_out_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                  contentDescription = stringResource(R.string.cd_sign_out),
                  tint = InkSecondary,
                )
              }
            }
          }

          if (isBusy) {
            LinearProgressIndicator(
              color = OrangeAccent,
              trackColor = ElevatedCard,
              modifier = Modifier.fillMaxWidth().height(2.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SectionTabPill(
          title = "Codes (${codes.size})",
          selected = selectedSection == "CODES",
          onClick = { selectedSection = "CODES" },
          modifier = Modifier.weight(1f).testTag("admin_tab_codes"),
        )
        SectionTabPill(
          title = "Abonnés (${profiles.size})",
          selected = selectedSection == "PROFILES",
          onClick = { selectedSection = "PROFILES" },
          modifier = Modifier.weight(1f).testTag("admin_tab_profiles"),
        )
        SectionTabPill(
          title = "Sources (${sources.size})",
          selected = selectedSection == "SOURCES",
          onClick = { selectedSection = "SOURCES" },
          modifier = Modifier.weight(1f).testTag("admin_tab_sources"),
        )
      }

      if (selectedSection == "CODES") {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          item(key = "generator_card") {
            Surface(
              color = ControlSurface,
              shape = RoundedCornerShape(18.dp),
              border = BorderStroke(1.dp, BorderSlate),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.admin_generate_title),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )

                OutlinedTextField(
                  value = customCodeInput,
                  onValueChange = { raw ->
                    if (raw.length <= 32) {
                      customCodeInput = AccessRepository.normalizeCode(raw)
                    }
                  },
                  label = { Text(stringResource(R.string.admin_custom_code_label)) },
                  placeholder = { Text("Ex: RIFI-VIP-KARIM") },
                  singleLine = true,
                  keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = CyanBroadcast,
                      unfocusedBorderColor = BorderSlate,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White,
                      focusedLabelColor = CyanBroadcast,
                      unfocusedLabelColor = InkSecondary,
                    ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("custom_code_input"),
                )

                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  SubscriptionPlan.entries.forEach { plan ->
                    val isTrial = plan == SubscriptionPlan.TRIAL_10M
                    Button(
                      onClick = {
                        val custom = customCodeInput.takeIf { it.isNotBlank() }
                        onGenerateCode(plan, custom)
                        if (custom != null) customCodeInput = ""
                      },
                      enabled = !isBusy,
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = if (isTrial) CyanBroadcast else OrangeAccent,
                          contentColor = Color.Black,
                        ),
                      shape = RoundedCornerShape(12.dp),
                      modifier =
                        Modifier
                          .height(48.dp)
                          .testTag("generate_code_${plan.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(text = plan.label, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          if (latestGeneratedCode != null) {
            item(key = "latest_generated") {
              Surface(
                color = EmeraldReady.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, EmeraldReady.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = "Dernier code généré (${latestGeneratedCode.planLabel})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldReady,
                  )
                  Text(
                    text = latestGeneratedCode.code,
                    style =
                      MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                      ),
                    color = Color.White,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = { copyCodeToClipboard(context, latestGeneratedCode.code) },
                      border = BorderStroke(1.dp, EmeraldReady),
                      modifier = Modifier.height(42.dp),
                    ) {
                      Text("Copier", color = EmeraldReady, fontWeight = FontWeight.Bold)
                    }
                    Button(
                      onClick = { shareActivationCode(context, latestGeneratedCode) },
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = EmeraldReady,
                          contentColor = Color.Black,
                        ),
                      modifier = Modifier.height(42.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Partager", fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          item(key = "code_filters") {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CodeFilterTab.entries.forEach { tab ->
                FilterChip(
                  selected = codeFilter == tab,
                  onClick = { codeFilter = tab },
                  label = { Text(tab.label) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = OrangeAccent.copy(alpha = 0.22f),
                      selectedLabelColor = OrangeAccent,
                      containerColor = ControlSurface,
                      labelColor = InkSecondary,
                    ),
                )
              }
            }
          }

          if (filteredCodes.isEmpty()) {
            item(key = "empty_codes") {
              EmptyAdminCard(text = stringResource(R.string.admin_empty_codes))
            }
          } else {
            items(filteredCodes, key = { it.code }) { item ->
              AdminCodeRowCard(
                code = item,
                serverNowMillis = serverNowMillis,
                onCopy = { copyCodeToClipboard(context, item.code) },
                onShare = { shareActivationCode(context, item) },
                onRevoke = { onRevokeCode(item) },
                onDelete = { onDeleteCode(item.code) },
              )
            }
          }
          item(key = "codes_bottom_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
        }
      } else if (selectedSection == "PROFILES") {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          if (profiles.isEmpty()) {
            item(key = "empty_profiles") {
              EmptyAdminCard(text = stringResource(R.string.admin_empty_profiles))
            }
          } else {
            items(profiles, key = { it.userId }) { profile ->
              AdminSubscriberCard(
                profile = profile,
                serverNowMillis = serverNowMillis,
                onToggleRevocation = { onToggleUserRevocation(profile) },
                onExtendPlan = { plan -> onExtendUserPlan(profile, plan) },
              )
            }
          }
          item(key = "profiles_bottom_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
        }
      } else {
        AdminSourcesSection(
          sources = sources,
          isBusy = isBusy,
          onAddSource = onAddSource,
          onToggleSource = onToggleSource,
          onDeleteSource = onDeleteSource,
          onSyncNow = onSyncSourcesNow,
          onOpenLocalImport = onOpenLocalImport,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

@Composable
private fun SectionTabPill(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    color = if (selected) OrangeAccent.copy(alpha = 0.2f) else ControlSurface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, if (selected) OrangeAccent else BorderSlate),
    modifier = modifier.height(48.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = if (selected) OrangeAccent else InkSecondary,
      )
    }
  }
}

@Composable
private fun AdminCodeRowCard(
  code: ActivationCode,
  serverNowMillis: Long,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  onRevoke: () -> Unit,
  onDelete: () -> Unit,
) {
  val status = code.parsedStatus()
  val remaining =
    if (status == ActivationCodeStatus.REDEEMED && code.expiresAtMillis > 0L) {
      code.expiresAtMillis - serverNowMillis
    } else {
      0L
    }
  val isExpired =
    status == ActivationCodeStatus.REDEEMED && code.expiresAtMillis > 0L && remaining <= 0L

  val (statusLabel, statusColor) =
    when {
      status == ActivationCodeStatus.REVOKED -> "RÉVOQUÉ" to CrimsonLive
      isExpired -> "EXPIRÉ" to AmberFavorite
      status == ActivationCodeStatus.REDEEMED ->
        "ACTIF (${formatRemainingDuration(remaining)})" to CyanBroadcast
      else -> "DISPONIBLE" to EmeraldReady
    }

  Surface(
    color = ControlSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, BorderSlate),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = code.code,
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            ),
          color = Color.White,
        )
        Surface(
          color = statusColor.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp),
          border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        ) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Formule : ${code.planLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = InkPrimary,
          )
          if (code.redeemedProfileName.isNotBlank()) {
            Text(
              text = "Profil : ${code.redeemedProfileName}",
              style = MaterialTheme.typography.labelSmall,
              color = InkSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          TextButton(onClick = onCopy) {
            Text("Copier", color = CyanBroadcast, style = MaterialTheme.typography.labelMedium)
          }
          IconButton(onClick = onShare) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = "Partager le code",
              tint = InkPrimary,
              modifier = Modifier.size(18.dp),
            )
          }
          if (status != ActivationCodeStatus.REVOKED) {
            IconButton(onClick = onRevoke) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Révoquer et couper l'accès",
                tint = AmberFavorite,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          IconButton(onClick = onDelete) {
            Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = "Supprimer le code",
              tint = CrimsonLive,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminSubscriberCard(
  profile: UserProfile,
  serverNowMillis: Long,
  onToggleRevocation: () -> Unit,
  onExtendPlan: (SubscriptionPlan) -> Unit,
) {
  val remaining = profile.expiresAtMillis - serverNowMillis
  val isExpired = remaining <= 0L
  val (statusLabel, statusColor) =
    when {
      profile.revoked -> "SUSPENDU" to CrimsonLive
      isExpired -> "EXPIRÉ" to AmberFavorite
      else -> "ACTIF • ${formatRemainingDuration(remaining)}" to EmeraldReady
    }

  Surface(
    color = ControlSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, BorderSlate),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = OrangeAccent,
            modifier = Modifier.size(20.dp),
          )
          Column {
            Text(
              text = profile.profileName,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
            Text(
              text = profile.email.ifBlank { profile.userId },
              style = MaterialTheme.typography.labelSmall,
              color = InkSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Surface(
          color = statusColor.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp),
          border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        ) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text = "Forfait : ${profile.planLabel}  •  Code : ${profile.activeCode}",
        style = MaterialTheme.typography.bodySmall,
        color = InkPrimary,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onToggleRevocation,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (profile.revoked) EmeraldReady else CrimsonLive,
              contentColor = if (profile.revoked) Color.Black else Color.White,
            ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(40.dp).testTag("toggle_suspend_${profile.userId}"),
        ) {
          Text(
            text = if (profile.revoked) "Réactiver" else "Suspendre l'abonnement",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          )
        }

        SubscriptionPlan.entries.forEach { plan ->
          OutlinedButton(
            onClick = { onExtendPlan(plan) },
            border = BorderStroke(1.dp, BorderSlate),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.height(40.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              tint = CyanBroadcast,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "+${plan.badge}",
              color = CyanBroadcast,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyAdminCard(text: String) {
  Surface(
    color = ControlSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, BorderSlate),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = InkSecondary,
      modifier = Modifier.padding(20.dp),
    )
  }
}

private fun copyCodeToClipboard(context: Context, code: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
  clipboard?.setPrimaryClip(ClipData.newPlainText("Code Rifi stream", code))
}

private fun shareActivationCode(context: Context, code: ActivationCode) {
  val message =
    "Voici votre code d'activation Rifi stream (${code.planLabel}) :\n${code.code}\nConnectez-vous avec votre e-mail puis saisissez ce code pour activer votre profil."
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, message)
    }
  context.startActivity(Intent.createChooser(intent, "Partager le code d'activation"))
}
