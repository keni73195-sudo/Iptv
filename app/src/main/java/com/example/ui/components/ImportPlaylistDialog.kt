package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.DefaultPlaylists
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ControlSurface
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanBroadcast
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg

@Composable
fun ImportPlaylistDialog(
  isImporting: Boolean,
  isSyncingSources: Boolean,
  importError: String?,
  onDismiss: () -> Unit,
  onSyncAllSources: () -> Unit,
  onImportFromUrl: (url: String, mergeWithCurrent: Boolean) -> Unit,
  onImportFromText: (rawM3u: String, mergeWithCurrent: Boolean) -> Unit,
  onResetDefault: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var urlInput by remember { mutableStateOf("") }
  var textInput by remember { mutableStateOf("") }
  var mergeWithCurrent by remember { mutableStateOf(true) }
  val busy = isImporting || isSyncingSources

  Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
    Surface(
      color = ControlSurface,
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, BorderSlate),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = stringResource(R.string.import_playlist_title),
          style = MaterialTheme.typography.titleLarge,
          color = InkPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Quick sync button for all 8 international playlists
        OutlinedButton(
          onClick = onSyncAllSources,
          enabled = !busy,
          border = BorderStroke(1.dp, CyanBroadcast.copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth().testTag("sync_all_sources_button"),
        ) {
          if (isSyncingSources) {
            CircularProgressIndicator(
              color = CyanBroadcast,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.syncing_sources_status),
              color = CyanBroadcast,
            )
          } else {
            Icon(
              painter = painterResource(R.drawable.ic_sync),
              contentDescription = null,
              tint = CyanBroadcast,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.sync_all_sources, DefaultPlaylists.REMOTE_SOURCES.size),
              color = CyanBroadcast,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Switcher: URL vs Raw #EXTM3U
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(
              0 to stringResource(R.string.import_url_tab),
              1 to stringResource(R.string.import_text_tab),
            )
            .forEach { (idx, title) ->
              val active = selectedTab == idx
              var isFocused by remember { mutableStateOf(false) }
              Surface(
                color = if (active) CyanBroadcast.copy(alpha = 0.18f) else ElevatedCard,
                shape = RoundedCornerShape(10.dp),
                border =
                  BorderStroke(
                    if (isFocused) 2.dp else 1.dp,
                    if (active || isFocused) CyanBroadcast else BorderSlate,
                  ),
                modifier =
                  Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable { selectedTab = idx }
                    .testTag("import_tab_$idx"),
              ) {
                Text(
                  text = title,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (active) CyanBroadcast else InkSecondary,
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                )
              }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
          OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            placeholder = {
              Text(
                text = stringResource(R.string.import_url_hint),
                color = InkMuted,
                maxLines = 1,
              )
            },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanBroadcast,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = InkPrimary,
                unfocusedTextColor = InkPrimary,
                cursorColor = CyanBroadcast,
              ),
            modifier = Modifier.fillMaxWidth().testTag("import_url_input"),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.sources_section_title),
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
          )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DefaultPlaylists.ALL_PRESETS, key = { it.id }) { preset ->
              var isFocused by remember { mutableStateOf(false) }
              Surface(
                color = ElevatedCard,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(if (isFocused) 2.dp else 1.dp, if (isFocused) CyanBroadcast else BorderSlate),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable { urlInput = preset.url }
                    .testTag("import_preset_${preset.id}"),
              ) {
                Text(
                  text = "+ ${preset.label}",
                  style = MaterialTheme.typography.labelMedium,
                  color = CyanBroadcast,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }
        } else {
          OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = {
              Text(
                text = stringResource(R.string.import_text_hint),
                color = InkMuted,
              )
            },
            minLines = 4,
            maxLines = 6,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanBroadcast,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = InkPrimary,
                unfocusedTextColor = InkPrimary,
                cursorColor = CyanBroadcast,
              ),
            modifier = Modifier.fillMaxWidth().testTag("import_raw_input"),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Replace vs Merge toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(
              false to stringResource(R.string.import_mode_replace),
              true to stringResource(R.string.import_mode_merge),
            )
            .forEach { (isMerge, label) ->
              val active = mergeWithCurrent == isMerge
              var isFocused by remember { mutableStateOf(false) }
              Surface(
                color = if (active) CyanBroadcast.copy(alpha = 0.14f) else ElevatedCard,
                shape = RoundedCornerShape(8.dp),
                border =
                  BorderStroke(
                    if (isFocused) 2.dp else 1.dp,
                    if (active || isFocused) CyanBroadcast else BorderSlate,
                  ),
                modifier =
                  Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable { mergeWithCurrent = isMerge },
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (active) CyanBroadcast else InkSecondary,
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                )
              }
            }
        }

        if (!importError.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = importError,
            style = MaterialTheme.typography.bodyMedium,
            color = CrimsonLive,
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onResetDefault,
            enabled = !busy,
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier.testTag("import_reset_button"),
          ) {
            Text(
              text = stringResource(R.string.import_reset_default),
              color = InkSecondary,
              style = MaterialTheme.typography.bodyMedium,
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss, enabled = !busy) {
              Text(text = stringResource(R.string.cancel), color = InkSecondary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = {
                if (selectedTab == 0) {
                  onImportFromUrl(urlInput, mergeWithCurrent)
                } else {
                  onImportFromText(textInput, mergeWithCurrent)
                }
              },
              enabled = !busy,
              colors = ButtonDefaults.buttonColors(containerColor = CyanBroadcast, contentColor = ObsidianBg),
              modifier = Modifier.testTag("import_confirm_button"),
            ) {
              if (isImporting) {
                CircularProgressIndicator(
                  color = ObsidianBg,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(16.dp),
                )
              } else {
                Text(text = stringResource(R.string.import_confirm))
              }
            }
          }
        }
      }
    }
  }
}
