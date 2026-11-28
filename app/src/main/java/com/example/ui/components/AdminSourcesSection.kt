package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DefaultPlaylists
import com.example.model.ChannelCategory
import com.example.model.RemotePlaylistSource
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

private val sourceCategoryOptions =
  listOf(
    null to "Auto",
    ChannelCategory.SPORTS to "Sport",
    ChannelCategory.MOVIES to "Cinéma",
    ChannelCategory.NEWS to "Info",
    ChannelCategory.ENTERTAINMENT to "Divertissement",
    ChannelCategory.KIDS to "Jeunesse",
    ChannelCategory.DOCUMENTARY to "Docs",
    ChannelCategory.MUSIC to "Musique",
    ChannelCategory.ADULT to "+18",
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSourcesSection(
  sources: List<RemotePlaylistSource>,
  isBusy: Boolean,
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
  onSyncNow: () -> Unit,
  onOpenLocalImport: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var labelInput by remember { mutableStateOf("") }
  var urlInput by remember { mutableStateOf("") }
  var tagInput by remember { mutableStateOf("LIVE") }
  var countryInput by remember { mutableStateOf("") }
  var languageInput by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<ChannelCategory?>(null) }
  var showBuiltinSources by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxSize(),
  ) {
    item(key = "add_remote_source_card") {
      Surface(
        color = ControlSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderSlate),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Ajouter une source IPTV à distance",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Text(
            text =
              "Toute source publiée ici est automatiquement synchronisée chez tous les abonnés actifs sans mise à jour de l'application.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = labelInput,
              onValueChange = { if (it.length <= 80) labelInput = it },
              label = { Text("Nom de la source") },
              placeholder = { Text("Ex: Sport VIP") },
              singleLine = true,
              colors = adminSourceFieldColors(),
              modifier = Modifier.weight(1.7f).testTag("admin_source_label_input"),
            )
            OutlinedTextField(
              value = tagInput,
              onValueChange = { if (it.length <= 16) tagInput = it.uppercase() },
              label = { Text("Tag") },
              placeholder = { Text("LIVE") },
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              colors = adminSourceFieldColors(),
              modifier = Modifier.weight(1f).testTag("admin_source_tag_input"),
            )
          }

          OutlinedTextField(
            value = urlInput,
            onValueChange = { if (it.length <= 500) urlInput = it },
            label = { Text("URL M3U / M3U8") },
            placeholder = { Text("https://exemple.com/playlist.m3u8") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = adminSourceFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("admin_source_url_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = countryInput,
              onValueChange = { if (it.length <= 60) countryInput = it },
              label = { Text("Pays (optionnel)") },
              placeholder = { Text("France") },
              singleLine = true,
              colors = adminSourceFieldColors(),
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = languageInput,
              onValueChange = { if (it.length <= 60) languageInput = it },
              label = { Text("Langue (optionnel)") },
              placeholder = { Text("Français") },
              singleLine = true,
              colors = adminSourceFieldColors(),
              modifier = Modifier.weight(1f),
            )
          }

          Text(
            text = "Catégorie forcée (optionnel) :",
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            sourceCategoryOptions.forEach { (category, chipLabel) ->
              val isSelected = selectedCategory == category
              FilterChip(
                selected = isSelected,
                onClick = { selectedCategory = category },
                label = {
                  Text(
                    text = chipLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  )
                },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangeAccent.copy(alpha = 0.22f),
                    selectedLabelColor = OrangeAccent,
                    containerColor = ElevatedCard,
                    labelColor = InkSecondary,
                  ),
                border =
                  FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = BorderSlate,
                    selectedBorderColor = OrangeAccent,
                  ),
              )
            }
          }

          Button(
            onClick = {
              onAddSource(
                labelInput,
                urlInput,
                tagInput,
                selectedCategory,
                countryInput,
                languageInput,
              )
              if (labelInput.isNotBlank() && urlInput.trim().startsWith("http", ignoreCase = true)) {
                labelInput = ""
                urlInput = ""
                countryInput = ""
                languageInput = ""
              }
            },
            enabled = !isBusy,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = OrangeAccent,
                contentColor = Color.Black,
              ),
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.fillMaxWidth().height(48.dp).testTag("admin_publish_source_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Publier pour tous les utilisateurs",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }

    item(key = "sources_quick_tools") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onSyncNow,
          border = BorderStroke(1.dp, CyanBroadcast),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = CyanBroadcast,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Synchroniser",
            color = CyanBroadcast,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          )
        }

        OutlinedButton(
          onClick = onOpenLocalImport,
          border = BorderStroke(1.dp, BorderSlate),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Text(
            text = "Import local M3U",
            color = InkPrimary,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          )
        }
      }
    }

    item(key = "published_sources_header") {
      Text(
        text = "Sources distantes publiées (${sources.size})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    if (sources.isEmpty()) {
      item(key = "empty_remote_sources") {
        Surface(
          color = ControlSurface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, BorderSlate),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text =
              "Aucune source ajoutée à distance pour l'instant. Ajoutez une URL M3U ci-dessus pour l'envoyer immédiatement à tous vos abonnés.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(18.dp),
          )
        }
      }
    } else {
      items(items = sources, key = { it.id }) { source ->
        RemoteSourceCard(
          source = source,
          onToggle = { onToggleSource(source) },
          onDelete = { onDeleteSource(source.id) },
        )
      }
    }

    item(key = "builtin_sources_card") {
      Surface(
        color = ControlSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Sources intégrées par défaut (${DefaultPlaylists.REMOTE_SOURCES.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Text(
                text = "Visibles uniquement par l'Admin",
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary,
              )
            }
            TextButton(onClick = { showBuiltinSources = !showBuiltinSources }) {
              Text(
                text = if (showBuiltinSources) "Masquer" else "Afficher",
                color = CyanBroadcast,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              )
            }
          }

          if (showBuiltinSources) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              DefaultPlaylists.REMOTE_SOURCES.forEach { preset ->
                Surface(
                  color = ObsidianBg,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.7f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                      text = "${preset.label} [${preset.countryTag}]",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = InkPrimary,
                    )
                    Text(
                      text = preset.url,
                      style =
                        MaterialTheme.typography.labelSmall.copy(
                          fontFamily = FontFamily.Monospace
                        ),
                      color = InkSecondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    item(key = "sources_bottom_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun RemoteSourceCard(
  source: RemotePlaylistSource,
  onToggle: () -> Unit,
  onDelete: () -> Unit,
) {
  val statusColor = if (source.enabled) EmeraldReady else AmberFavorite
  val statusText = if (source.enabled) "ACTIVE" else "DÉSACTIVÉE"

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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${source.label}  •  ${source.countryTag}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = source.url,
            style =
              MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = CyanBroadcast,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Surface(
          color = statusColor.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp),
          border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        ) {
          Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      val metadataTags =
        listOfNotNull(
          source.forcedCategory.takeIf { it.isNotBlank() }?.let { "Catégorie: $it" },
          source.forcedCountry.takeIf { it.isNotBlank() }?.let { "Pays: $it" },
          source.forcedLanguage.takeIf { it.isNotBlank() }?.let { "Langue: $it" },
        )
      if (metadataTags.isNotEmpty()) {
        Text(
          text = metadataTags.joinToString("  •  "),
          style = MaterialTheme.typography.labelSmall,
          color = InkSecondary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Switch(
            checked = source.enabled,
            onCheckedChange = { onToggle() },
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = EmeraldReady,
                uncheckedThumbColor = InkSecondary,
                uncheckedTrackColor = ElevatedCard,
              ),
          )
          Text(
            text = if (source.enabled) "Visible chez les abonnés" else "Masquée",
            style = MaterialTheme.typography.bodySmall,
            color = InkPrimary,
          )
        }

        IconButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = "Supprimer la source",
            tint = CrimsonLive,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun adminSourceFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = OrangeAccent,
    unfocusedBorderColor = BorderSlate,
    focusedLabelColor = OrangeAccent,
    unfocusedLabelColor = InkSecondary,
    cursorColor = OrangeAccent,
  )
