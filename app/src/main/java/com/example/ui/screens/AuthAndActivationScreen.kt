package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AccessRepository
import com.example.model.AccessGateState
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ControlSurface
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanBroadcast
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OrangeAccent

@Composable
fun AuthAndActivationScreen(
  gateState: AccessGateState,
  isTelevision: Boolean,
  onToggleAuthMode: (Boolean) -> Unit,
  onSubmitEmailPassword: (email: String, password: String, isCreateAccount: Boolean) -> Unit,
  onSendPasswordReset: (email: String) -> Unit,
  onSubmitActivationCode: (profileName: String, code: String) -> Unit,
  onResendAdminVerification: () -> Unit,
  onRefreshAdminVerification: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().background(ObsidianBg)) {
    Image(
      painter = painterResource(R.drawable.login_bg),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to ObsidianBg.copy(alpha = 0.50f),
              0.45f to ObsidianBg.copy(alpha = 0.68f),
              1.0f to ObsidianBg.copy(alpha = 0.90f),
            )
          )
    )

    BoxWithConstraints(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = if (isTelevision) 40.dp else 20.dp, vertical = 24.dp),
      contentAlignment = Alignment.Center,
    ) {
      val isWide = maxWidth >= 720.dp || isTelevision
      if (isWide) {
        Row(
          modifier = Modifier.fillMaxWidth().widthIn(max = 1060.dp),
          horizontalArrangement = Arrangement.spacedBy(32.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          BrandWelcomeHero(
            compact = false,
            modifier = Modifier.weight(1f),
          )
          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
          ) {
            GateCard(
              gateState = gateState,
              onToggleAuthMode = onToggleAuthMode,
              onSubmitEmailPassword = onSubmitEmailPassword,
              onSendPasswordReset = onSendPasswordReset,
              onSubmitActivationCode = onSubmitActivationCode,
              onResendAdminVerification = onResendAdminVerification,
              onRefreshAdminVerification = onRefreshAdminVerification,
              onSignOut = onSignOut,
              modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp),
            )
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          BrandWelcomeHero(
            compact = true,
            modifier = Modifier.fillMaxWidth(),
          )
          GateCard(
            gateState = gateState,
            onToggleAuthMode = onToggleAuthMode,
            onSubmitEmailPassword = onSubmitEmailPassword,
            onSendPasswordReset = onSendPasswordReset,
            onSubmitActivationCode = onSubmitActivationCode,
            onResendAdminVerification = onResendAdminVerification,
            onRefreshAdminVerification = onRefreshAdminVerification,
            onSignOut = onSignOut,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun BrandWelcomeHero(
  compact: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        color = OrangeAccent.copy(alpha = 0.2f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.6f)),
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = OrangeAccent,
            modifier = Modifier.size(28.dp),
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(R.string.app_name),
          style =
            (if (compact) MaterialTheme.typography.headlineMedium
              else MaterialTheme.typography.displaySmall)
              .copy(fontWeight = FontWeight.ExtraBold),
          color = Color.White,
        )
        Box(
          modifier =
            Modifier
              .padding(start = 5.dp, top = 8.dp)
              .size(9.dp)
              .clip(CircleShape)
              .background(OrangeAccent)
        )
      }
    }

    Text(
      text = stringResource(R.string.auth_hero_title),
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = InkPrimary,
      textAlign = if (compact) TextAlign.Center else TextAlign.Start,
    )

    Text(
      text = stringResource(R.string.auth_hero_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = InkPrimary.copy(alpha = 0.85f),
      textAlign = if (compact) TextAlign.Center else TextAlign.Start,
    )
  }
}

@Composable
private fun GateCard(
  gateState: AccessGateState,
  onToggleAuthMode: (Boolean) -> Unit,
  onSubmitEmailPassword: (email: String, password: String, isCreateAccount: Boolean) -> Unit,
  onSendPasswordReset: (email: String) -> Unit,
  onSubmitActivationCode: (profileName: String, code: String) -> Unit,
  onResendAdminVerification: () -> Unit,
  onRefreshAdminVerification: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ControlSurface.copy(alpha = 0.91f),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.9f)),
    shadowElevation = 16.dp,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      when (gateState) {
        is AccessGateState.Loading -> {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CircularProgressIndicator(color = OrangeAccent)
            Text(
              text = stringResource(R.string.auth_checking_access),
              style = MaterialTheme.typography.bodyMedium,
              color = InkSecondary,
            )
          }
        }

        is AccessGateState.NeedsEmailAuth -> {
          var emailInput by rememberSaveable { mutableStateOf("") }
          var passwordInput by rememberSaveable { mutableStateOf("") }
          var showPassword by remember { mutableStateOf(false) }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AuthModeTab(
              label = stringResource(R.string.auth_tab_login),
              selected = !gateState.isCreateAccount,
              onClick = { onToggleAuthMode(false) },
              modifier = Modifier.weight(1f).testTag("auth_mode_login"),
            )
            AuthModeTab(
              label = stringResource(R.string.auth_tab_register),
              selected = gateState.isCreateAccount,
              onClick = { onToggleAuthMode(true) },
              modifier = Modifier.weight(1f).testTag("auth_mode_register"),
            )
          }

          Text(
            text =
              if (gateState.isCreateAccount) stringResource(R.string.auth_register_subtitle)
              else stringResource(R.string.auth_login_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
          )

          gateState.bannerMessage?.let { StatusAlertBanner(text = it, isError = false) }
          gateState.errorMessage?.let { StatusAlertBanner(text = it, isError = true) }

          OutlinedTextField(
            value = emailInput,
            onValueChange = { if (it.length <= 160) emailInput = it },
            label = { Text(stringResource(R.string.auth_email_label)) },
            placeholder = { Text(stringResource(R.string.auth_email_hint)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Email,
                contentDescription = null,
                tint = OrangeAccent,
              )
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
              ),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeAccent,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = OrangeAccent,
                unfocusedLabelColor = InkSecondary,
                cursorColor = OrangeAccent,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("email_input"),
          )

          OutlinedTextField(
            value = passwordInput,
            onValueChange = { if (it.length <= 64) passwordInput = it },
            label = { Text(stringResource(R.string.auth_password_label)) },
            placeholder = { Text(stringResource(R.string.auth_password_hint)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = CyanBroadcast,
              )
            },
            trailingIcon = {
              TextButton(onClick = { showPassword = !showPassword }) {
                Text(
                  text = if (showPassword) "Masquer" else "Afficher",
                  color = InkSecondary,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
            },
            singleLine = true,
            visualTransformation =
              if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
              ),
            keyboardActions =
              KeyboardActions(
                onDone = {
                  if (!gateState.isSubmitting) {
                    onSubmitEmailPassword(emailInput, passwordInput, gateState.isCreateAccount)
                  }
                }
              ),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanBroadcast,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = CyanBroadcast,
                unfocusedLabelColor = InkSecondary,
                cursorColor = CyanBroadcast,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("password_input"),
          )

          if (!gateState.isCreateAccount) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              TextButton(
                onClick = { onSendPasswordReset(emailInput) },
                enabled = !gateState.isSubmitting,
              ) {
                Text(
                  text = stringResource(R.string.auth_forgot_password),
                  color = CyanBroadcast,
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            }
          }

          var submitFocused by remember { mutableStateOf(false) }
          Button(
            onClick = {
              onSubmitEmailPassword(emailInput, passwordInput, gateState.isCreateAccount)
            },
            enabled = !gateState.isSubmitting,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = OrangeAccent,
                contentColor = Color.Black,
                disabledContainerColor = ElevatedCard,
                disabledContentColor = InkMuted,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier
                .fillMaxWidth()
                .height(54.dp)
                .onFocusChanged { submitFocused = it.isFocused }
                .then(
                  if (submitFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(14.dp))
                  else Modifier
                )
                .testTag("email_auth_submit_button"),
          ) {
            if (gateState.isSubmitting) {
              CircularProgressIndicator(
                color = Color.Black,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.activation_verifying),
                fontWeight = FontWeight.Bold,
              )
            } else {
              Text(
                text =
                  if (gateState.isCreateAccount) stringResource(R.string.auth_submit_register)
                  else stringResource(R.string.auth_submit_login),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              )
            }
          }
        }

        is AccessGateState.NeedsAdminVerification -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = gateState.email,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = CyanBroadcast,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSignOut) {
              Text(stringResource(R.string.cd_sign_out), color = InkSecondary)
            }
          }

          Text(
            text = "Vérification de votre adresse",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )

          gateState.infoMessage?.let { StatusAlertBanner(text = it, isError = false) }
          gateState.errorMessage?.let { StatusAlertBanner(text = it, isError = true) }

          Button(
            onClick = onRefreshAdminVerification,
            enabled = !gateState.isSending,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = OrangeAccent,
                contentColor = Color.Black,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp),
          ) {
            Text("J'ai vérifié mon e-mail", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onResendAdminVerification,
            enabled = !gateState.isSending,
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Text("Renvoyer le lien", color = InkPrimary)
          }
        }

        is AccessGateState.NeedsActivation -> {
          var profileNameInput by
            rememberSaveable(gateState.uid, gateState.suggestedName) {
              mutableStateOf(gateState.suggestedName)
            }
          var activationCodeInput by rememberSaveable(gateState.uid) { mutableStateOf("") }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = EmeraldReady,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = gateState.email.ifBlank { gateState.uid },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            TextButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                contentDescription = stringResource(R.string.cd_sign_out),
                tint = InkSecondary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = stringResource(R.string.auth_switch_account),
                color = InkSecondary,
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }

          gateState.bannerMessage?.let { StatusAlertBanner(text = it, isError = true) }

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = stringResource(R.string.activation_title),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
            Text(
              text = stringResource(R.string.activation_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = InkSecondary,
            )
          }

          OutlinedTextField(
            value = profileNameInput,
            onValueChange = { if (it.length <= 40) profileNameInput = it },
            label = { Text(stringResource(R.string.activation_profile_label)) },
            placeholder = { Text(stringResource(R.string.activation_profile_hint)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = OrangeAccent,
              )
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
              ),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeAccent,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = OrangeAccent,
                unfocusedLabelColor = InkSecondary,
                cursorColor = OrangeAccent,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
          )

          OutlinedTextField(
            value = activationCodeInput,
            onValueChange = { raw ->
              if (raw.length <= 32) {
                activationCodeInput = AccessRepository.normalizeCode(raw)
              }
            },
            label = { Text(stringResource(R.string.activation_code_label)) },
            placeholder = { Text(stringResource(R.string.activation_code_hint)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = CyanBroadcast,
              )
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
              ),
            keyboardActions =
              KeyboardActions(
                onDone = {
                  if (!gateState.isSubmitting &&
                    profileNameInput.trim().length >= 2 &&
                    activationCodeInput.trim().length >= 6
                  ) {
                    onSubmitActivationCode(profileNameInput, activationCodeInput)
                  }
                }
              ),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanBroadcast,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = CyanBroadcast,
                unfocusedLabelColor = InkSecondary,
                cursorColor = CyanBroadcast,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("activation_code_input"),
          )

          gateState.errorMessage?.let { StatusAlertBanner(text = it, isError = true) }

          var submitFocused by remember { mutableStateOf(false) }
          val canSubmit =
            !gateState.isSubmitting &&
              profileNameInput.trim().length >= 2 &&
              activationCodeInput.trim().length >= 6

          Button(
            onClick = { onSubmitActivationCode(profileNameInput, activationCodeInput) },
            enabled = canSubmit,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = OrangeAccent,
                contentColor = Color.Black,
                disabledContainerColor = ElevatedCard,
                disabledContentColor = InkMuted,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier
                .fillMaxWidth()
                .height(54.dp)
                .onFocusChanged { submitFocused = it.isFocused }
                .then(
                  if (submitFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(14.dp))
                  else Modifier
                )
                .testTag("activate_code_button"),
          ) {
            if (gateState.isSubmitting) {
              CircularProgressIndicator(
                color = Color.Black,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.activation_verifying),
                fontWeight = FontWeight.Bold,
              )
            } else {
              Text(
                text = stringResource(R.string.activation_submit_button),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              )
            }
          }
        }

        is AccessGateState.Authorized -> Unit
      }
    }
  }
}

@Composable
private fun AuthModeTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    color = if (selected) OrangeAccent.copy(alpha = 0.22f) else ElevatedCard,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, if (selected) OrangeAccent else BorderSlate),
    modifier = modifier.height(44.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = if (selected) OrangeAccent else InkSecondary,
      )
    }
  }
}

@Composable
private fun StatusAlertBanner(
  text: String,
  isError: Boolean,
) {
  val accent = if (isError) CrimsonLive else CyanBroadcast
  Surface(
    color = accent.copy(alpha = 0.16f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Warning,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = Color.White,
      )
    }
  }
}
