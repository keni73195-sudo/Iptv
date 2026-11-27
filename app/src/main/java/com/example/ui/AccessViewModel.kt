package com.example.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccessRepository
import com.example.model.AccessGateState
import com.example.model.ActivationCode
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccessViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = AccessRepository(application.applicationContext)

  private val _gateState = MutableStateFlow<AccessGateState>(AccessGateState.Loading)
  val gateState: StateFlow<AccessGateState> = _gateState.asStateFlow()

  private val _isAdminPanelOpen = MutableStateFlow(false)
  val isAdminPanelOpen: StateFlow<Boolean> = _isAdminPanelOpen.asStateFlow()

  private val _adminCodes = MutableStateFlow<List<ActivationCode>>(emptyList())
  val adminCodes: StateFlow<List<ActivationCode>> = _adminCodes.asStateFlow()

  private val _adminProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
  val adminProfiles: StateFlow<List<UserProfile>> = _adminProfiles.asStateFlow()

  private val _adminActionBusy = MutableStateFlow(false)
  val adminActionBusy: StateFlow<Boolean> = _adminActionBusy.asStateFlow()

  private val _adminBanner = MutableStateFlow<String?>(null)
  val adminBanner: StateFlow<String?> = _adminBanner.asStateFlow()

  private val _latestGeneratedCode = MutableStateFlow<ActivationCode?>(null)
  val latestGeneratedCode: StateFlow<ActivationCode?> = _latestGeneratedCode.asStateFlow()

  private var profileObserverJob: Job? = null
  private var tickerJob: Job? = null
  private var adminObserverJob: Job? = null
  private var currentIsAdmin = false
  private var latestObservedProfile: UserProfile? = null

  init {
    val currentUser = repository.auth.currentUser
    if (currentUser != null) {
      startSessionForUser(currentUser)
    } else {
      _gateState.value = AccessGateState.NeedsEmailAuth()
    }
  }

  fun toggleAuthMode(isCreateAccount: Boolean) {
    val cur = _gateState.value as? AccessGateState.NeedsEmailAuth ?: return
    if (cur.isSubmitting) return
    _gateState.value =
      cur.copy(isCreateAccount = isCreateAccount, errorMessage = null, bannerMessage = null)
  }

  fun submitEmailPassword(emailOrUsername: String, password: String, isCreateAccount: Boolean) {
    val cleanEmail = repository.resolveLoginIdentifier(emailOrUsername)
    if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      _gateState.value =
        AccessGateState.NeedsEmailAuth(
          isCreateAccount = isCreateAccount,
          errorMessage = "Veuillez saisir une adresse e-mail ou un identifiant valide.",
        )
      return
    }
    if (password.length < 6) {
      _gateState.value =
        AccessGateState.NeedsEmailAuth(
          isCreateAccount = isCreateAccount,
          errorMessage = "Le mot de passe doit contenir au moins 6 caractères.",
        )
      return
    }

    _gateState.value =
      AccessGateState.NeedsEmailAuth(
        isCreateAccount = isCreateAccount,
        isSubmitting = true,
      )

    viewModelScope.launch {
      val res =
        if (isCreateAccount) {
          repository.createAccountWithEmailPassword(cleanEmail, password)
        } else {
          repository.signInWithEmailPassword(cleanEmail, password)
        }

      res.fold(
        onSuccess = { user ->
          if (isCreateAccount && repository.isBootstrappedAdminEmail(user.email) && !user.isEmailVerified) {
            repository.sendAdminVerificationEmail(user)
          }
          startSessionForUser(user, openAdminIfEligible = true)
        },
        onFailure = { error ->
          _gateState.value =
            AccessGateState.NeedsEmailAuth(
              isCreateAccount = isCreateAccount,
              isSubmitting = false,
              errorMessage = error.localizedMessage ?: "Échec de la connexion.",
            )
        },
      )
    }
  }

  fun sendPasswordReset(emailOrUsername: String) {
    val cleanEmail = repository.resolveLoginIdentifier(emailOrUsername)
    val cur = _gateState.value as? AccessGateState.NeedsEmailAuth
    val isCreate = cur?.isCreateAccount ?: false
    if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      _gateState.value =
        AccessGateState.NeedsEmailAuth(
          isCreateAccount = isCreate,
          errorMessage = "Saisissez d'abord votre e-mail ou identifiant pour réinitialiser le mot de passe.",
        )
      return
    }

    _gateState.value =
      AccessGateState.NeedsEmailAuth(isCreateAccount = isCreate, isSubmitting = true)

    viewModelScope.launch {
      val res = repository.sendPasswordReset(cleanEmail)
      res.fold(
        onSuccess = {
          _gateState.value =
            AccessGateState.NeedsEmailAuth(
              isCreateAccount = false,
              isSubmitting = false,
              bannerMessage = "Un lien de réinitialisation a été envoyé à $cleanEmail.",
            )
        },
        onFailure = { error ->
          _gateState.value =
            AccessGateState.NeedsEmailAuth(
              isCreateAccount = isCreate,
              isSubmitting = false,
              errorMessage = error.localizedMessage ?: "Impossible d'envoyer l'e-mail.",
            )
        },
      )
    }
  }

  fun resendAdminVerification() {
    val user = repository.auth.currentUser ?: return
    _gateState.value =
      AccessGateState.NeedsAdminVerification(
        uid = user.uid,
        email = user.email.orEmpty(),
        isSending = true,
      )
    viewModelScope.launch {
      val res = repository.sendAdminVerificationEmail(user)
      res.fold(
        onSuccess = {
          _gateState.value =
            AccessGateState.NeedsAdminVerification(
              uid = user.uid,
              email = user.email.orEmpty(),
              isSending = false,
              infoMessage = "E-mail envoyé à ${user.email}. Vérifiez votre boîte puis validez.",
            )
        },
        onFailure = { err ->
          _gateState.value =
            AccessGateState.NeedsAdminVerification(
              uid = user.uid,
              email = user.email.orEmpty(),
              isSending = false,
              errorMessage = err.localizedMessage ?: "Envoi impossible pour le moment.",
            )
        },
      )
    }
  }

  fun refreshAdminVerification() {
    val user = repository.auth.currentUser ?: return
    _gateState.value =
      AccessGateState.NeedsAdminVerification(
        uid = user.uid,
        email = user.email.orEmpty(),
        isSending = true,
      )
    viewModelScope.launch {
      val reload = repository.reloadCurrentUser()
      reload.fold(
        onSuccess = { refreshed ->
          if (refreshed != null && refreshed.isEmailVerified) {
            startSessionForUser(refreshed, openAdminIfEligible = true)
          } else {
            _gateState.value =
              AccessGateState.NeedsAdminVerification(
                uid = user.uid,
                email = user.email.orEmpty(),
                isSending = false,
                errorMessage = "E-mail pas encore confirmé. Cliquez sur le lien reçu.",
              )
          }
        },
        onFailure = { err ->
          _gateState.value =
            AccessGateState.NeedsAdminVerification(
              uid = user.uid,
              email = user.email.orEmpty(),
              isSending = false,
              errorMessage = err.localizedMessage ?: "Vérification impossible.",
            )
        },
      )
    }
  }

  private fun startSessionForUser(
    user: FirebaseUser,
    openAdminIfEligible: Boolean = false,
  ) {
    cancelSessionObservers()
    _gateState.value = AccessGateState.Loading

    profileObserverJob =
      viewModelScope.launch {
        val email = user.email.orEmpty()
        val isAdmin = repository.checkIsAdmin(user.uid, user.email, user.isEmailVerified)
        if (!isAdmin && repository.isBootstrappedAdminEmail(user.email) && !user.isEmailVerified) {
          currentIsAdmin = false
          repository.sendAdminVerificationEmail(user)
          _gateState.value =
            AccessGateState.NeedsAdminVerification(
              uid = user.uid,
              email = email,
              infoMessage =
                "Cliquez sur le lien de confirmation envoyé à $email puis validez pour accéder à l'espace gestion.",
            )
          return@launch
        }

        currentIsAdmin = isAdmin
        repository.syncServerTime(user.uid)

        if (isAdmin) {
          evaluateAccessState(user, null, true)
          if (openAdminIfEligible) {
            openAdminPanel()
          }
          return@launch
        }

        repository.observeUserProfile(user.uid).collect { result ->
          result.fold(
            onSuccess = { profile ->
              latestObservedProfile = profile
              evaluateAccessState(user, profile, false)
            },
            onFailure = { error ->
              tickerJob?.cancel()
              _gateState.value =
                AccessGateState.NeedsActivation(
                  uid = user.uid,
                  email = email,
                  suggestedName = latestObservedProfile?.profileName.orEmpty(),
                  previousProfile = latestObservedProfile,
                  errorMessage = error.localizedMessage ?: "Impossible de vérifier votre accès.",
                )
            },
          )
        }
      }
  }

  private fun evaluateAccessState(
    user: FirebaseUser,
    profile: UserProfile?,
    isAdmin: Boolean,
  ) {
    val email = user.email.orEmpty()
    val suggestedName =
      profile?.profileName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@").take(24)

    if (isAdmin) {
      tickerJob?.cancel()
      _gateState.value =
        AccessGateState.Authorized(
          uid = user.uid,
          email = email,
          isAdmin = true,
          profile = profile,
          remainingMillis = 0L,
        )
      return
    }

    if (profile == null) {
      tickerJob?.cancel()
      _gateState.value =
        AccessGateState.NeedsActivation(
          uid = user.uid,
          email = email,
          suggestedName = suggestedName,
        )
      return
    }

    if (profile.revoked) {
      tickerJob?.cancel()
      _gateState.value =
        AccessGateState.NeedsActivation(
          uid = user.uid,
          email = email,
          suggestedName = suggestedName,
          previousProfile = profile,
          bannerMessage =
            "Votre abonnement a été suspendu. Vous avez été déconnecté.",
        )
      return
    }

    val remaining = profile.expiresAtMillis - repository.currentServerNowMillis()
    if (remaining <= 0L) {
      tickerJob?.cancel()
      _gateState.value =
        AccessGateState.NeedsActivation(
          uid = user.uid,
          email = email,
          suggestedName = suggestedName,
          previousProfile = profile,
          bannerMessage =
            "Votre abonnement (${profile.planLabel}) est expiré. Vous avez été déconnecté automatiquement.",
        )
      return
    }

    _gateState.value =
      AccessGateState.Authorized(
        uid = user.uid,
        email = email,
        isAdmin = false,
        profile = profile,
        remainingMillis = remaining,
      )
    startExpirationTicker(user, profile)
  }

  private fun startExpirationTicker(user: FirebaseUser, profile: UserProfile) {
    tickerJob?.cancel()
    tickerJob =
      viewModelScope.launch {
        while (isActive) {
          delay(1_000L)
          val remaining = profile.expiresAtMillis - repository.currentServerNowMillis()
          if (remaining <= 0L) {
            _gateState.value =
              AccessGateState.NeedsActivation(
                uid = user.uid,
                email = user.email.orEmpty(),
                suggestedName = profile.profileName,
                previousProfile = profile,
                bannerMessage =
                  "Votre abonnement (${profile.planLabel}) est expiré. Vous avez été déconnecté automatiquement.",
              )
            break
          } else {
            val cur = _gateState.value
            if (cur is AccessGateState.Authorized && !cur.isAdmin) {
              _gateState.value = cur.copy(remainingMillis = remaining)
            }
          }
        }
      }
  }

  fun submitActivationCode(profileName: String, rawCode: String) {
    val user = repository.auth.currentUser
    if (user == null) {
      _gateState.value =
        AccessGateState.NeedsEmailAuth(
          errorMessage = "Veuillez d'abord vous connecter."
        )
      return
    }

    val prevState = _gateState.value as? AccessGateState.NeedsActivation
    _gateState.value =
      AccessGateState.NeedsActivation(
        uid = user.uid,
        email = user.email.orEmpty(),
        suggestedName = profileName,
        previousProfile = prevState?.previousProfile,
        isSubmitting = true,
      )

    viewModelScope.launch {
      val result =
        repository.redeemActivationCode(
          uid = user.uid,
          email = user.email.orEmpty(),
          profileName = profileName,
          rawCode = rawCode,
        )
      result.fold(
        onSuccess = { createdProfile ->
          latestObservedProfile = createdProfile
          evaluateAccessState(user, createdProfile, currentIsAdmin)
        },
        onFailure = { error ->
          _gateState.value =
            AccessGateState.NeedsActivation(
              uid = user.uid,
              email = user.email.orEmpty(),
              suggestedName = profileName,
              previousProfile = prevState?.previousProfile,
              errorMessage = error.localizedMessage ?: "Échec de l'activation du code.",
              isSubmitting = false,
            )
        },
      )
    }
  }

  fun openAdminPanel() {
    if (!currentIsAdmin) return
    _isAdminPanelOpen.value = true
    startAdminObservers()
  }

  fun closeAdminPanel() {
    _isAdminPanelOpen.value = false
    adminObserverJob?.cancel()
    adminObserverJob = null
  }

  private fun startAdminObservers() {
    adminObserverJob?.cancel()
    adminObserverJob =
      viewModelScope.launch {
        launch {
          repository.observeAllActivationCodes().collect { result ->
            result.fold(
              onSuccess = { _adminCodes.value = it },
              onFailure = { err ->
                _adminBanner.value = err.localizedMessage ?: "Erreur chargement des codes."
              },
            )
          }
        }
        launch {
          repository.observeAllUserProfiles().collect { result ->
            result.fold(
              onSuccess = { _adminProfiles.value = it },
              onFailure = { err ->
                _adminBanner.value = err.localizedMessage ?: "Erreur chargement des abonnés."
              },
            )
          }
        }
      }
  }

  fun generateCode(plan: SubscriptionPlan, customCode: String? = null) {
    val uid = repository.auth.currentUser?.uid ?: return
    if (!currentIsAdmin) return

    _adminActionBusy.value = true
    viewModelScope.launch {
      val result = repository.createActivationCode(uid, plan, customCode)
      _adminActionBusy.value = false
      result.fold(
        onSuccess = { created ->
          _latestGeneratedCode.value = created
          _adminBanner.value = "Code créé (${created.planLabel}) : ${created.code}"
        },
        onFailure = { err ->
          _adminBanner.value = err.localizedMessage ?: "Impossible de créer le code."
        },
      )
    }
  }

  fun revokeCode(code: ActivationCode) {
    if (!currentIsAdmin) return
    _adminActionBusy.value = true
    viewModelScope.launch {
      val res = repository.revokeCodeAndProfile(code)
      _adminActionBusy.value = false
      res.fold(
        onSuccess = { _adminBanner.value = "Code ${code.code} révoqué et accès coupé." },
        onFailure = { err -> _adminBanner.value = err.localizedMessage ?: "Erreur révocation." },
      )
    }
  }

  fun deleteCode(codeId: String) {
    if (!currentIsAdmin) return
    _adminActionBusy.value = true
    viewModelScope.launch {
      val res = repository.deleteActivationCode(codeId)
      _adminActionBusy.value = false
      res.fold(
        onSuccess = { _adminBanner.value = "Code $codeId supprimé." },
        onFailure = { err -> _adminBanner.value = err.localizedMessage ?: "Erreur suppression." },
      )
    }
  }

  fun toggleUserRevocation(profile: UserProfile) {
    if (!currentIsAdmin) return
    _adminActionBusy.value = true
    viewModelScope.launch {
      val nextRevoked = !profile.revoked
      val res = repository.setUserRevoked(profile.userId, nextRevoked)
      _adminActionBusy.value = false
      res.fold(
        onSuccess = {
          _adminBanner.value =
            if (nextRevoked) "Abonnement de ${profile.profileName} suspendu."
            else "Abonnement de ${profile.profileName} réactivé."
        },
        onFailure = { err -> _adminBanner.value = err.localizedMessage ?: "Erreur mise à jour." },
      )
    }
  }

  fun extendUserPlan(profile: UserProfile, plan: SubscriptionPlan) {
    val adminUid = repository.auth.currentUser?.uid ?: return
    if (!currentIsAdmin) return
    _adminActionBusy.value = true
    viewModelScope.launch {
      val res = repository.extendUserSubscription(profile, plan, adminUid)
      _adminActionBusy.value = false
      res.fold(
        onSuccess = {
          _adminBanner.value =
            "Abonnement de ${profile.profileName} prolongé (${plan.label})."
        },
        onFailure = { err -> _adminBanner.value = err.localizedMessage ?: "Erreur prolongation." },
      )
    }
  }

  fun clearAdminBanner() {
    _adminBanner.value = null
  }

  fun serverNowMillis(): Long = repository.currentServerNowMillis()

  fun signOut(bannerMessage: String? = null) {
    viewModelScope.launch {
      withContext(NonCancellable) {
        cancelSessionObservers()
        adminObserverJob?.cancel()
        adminObserverJob = null
        _isAdminPanelOpen.value = false
        _adminCodes.value = emptyList()
        _adminProfiles.value = emptyList()
        currentIsAdmin = false
        latestObservedProfile = null
        repository.auth.signOut()
      }
      _gateState.value = AccessGateState.NeedsEmailAuth(bannerMessage = bannerMessage)
    }
  }

  private fun cancelSessionObservers() {
    profileObserverJob?.cancel()
    profileObserverJob = null
    tickerJob?.cancel()
    tickerJob = null
  }

  override fun onCleared() {
    cancelSessionObservers()
    adminObserverJob?.cancel()
    super.onCleared()
  }
}
