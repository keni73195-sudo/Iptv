package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.example.R
import com.example.model.ActivationCode
import com.example.model.ActivationCodeStatus
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.security.SecureRandom
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class AccessRepository(private val context: Context) {
  init {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
  }

  val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val db: FirebaseFirestore by lazy {
    FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
  }

  private val random = SecureRandom()

  @Volatile
  private var trustedServerOffsetMs: Long? = null

  fun currentServerNowMillis(): Long {
    val offset = trustedServerOffsetMs
    return if (offset != null) {
      SystemClock.elapsedRealtime() + offset
    } else {
      System.currentTimeMillis()
    }
  }

  @SuppressLint("HardwareIds")
  fun getDeviceIdentifier(): String {
    val androidId =
      runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
      }.getOrNull()
    val cleaned = androidId?.trim()?.takeIf { it.length >= 4 } ?: "rifi_device_default"
    return cleaned.take(128)
  }

  fun isBootstrappedAdminEmail(email: String?): Boolean =
    email != null && email.trim().equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)

  fun resolveLoginIdentifier(identifier: String): String {
    val cleaned = identifier.trim()
    return if (cleaned.equals(ADMIN_USERNAME, ignoreCase = true)) {
      BOOTSTRAPPED_ADMIN_EMAIL
    } else {
      cleaned
    }
  }

  suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
      val user = result.user ?: return Result.failure(IllegalStateException("Session introuvable."))
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(translateAuthError(e)))
    }
  }

  suspend fun createAccountWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
      val user = result.user ?: return Result.failure(IllegalStateException("Création échouée."))
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(translateAuthError(e)))
    }
  }

  suspend fun sendPasswordReset(email: String): Result<Unit> {
    return try {
      auth.sendPasswordResetEmail(email.trim()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(translateAuthError(e)))
    }
  }

  suspend fun sendAdminVerificationEmail(user: FirebaseUser): Result<Unit> {
    return try {
      user.sendEmailVerification().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(translateAuthError(e)))
    }
  }

  suspend fun reloadCurrentUser(): Result<FirebaseUser?> {
    return try {
      val user = auth.currentUser ?: return Result.success(null)
      user.reload().await()
      Result.success(auth.currentUser)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun checkIsAdmin(uid: String, email: String?, emailVerified: Boolean): Boolean {
    if (isBootstrappedAdminEmail(email) && emailVerified) {
      return true
    }
    return try {
      val doc = db.collection(COLLECTION_ADMINS).document(uid).get().await()
      doc.exists()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  suspend fun syncServerTime(uid: String): Long {
    val path = "$COLLECTION_TIME_SYNC/$uid"
    return try {
      val docRef = db.collection(COLLECTION_TIME_SYNC).document(uid)
      docRef.set(
        mapOf(
          "userId" to uid,
          "syncedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      val serverSnap = docRef.get(Source.SERVER).await()
      val ts = serverSnap.getTimestamp("syncedAt")
      val serverNow = ts?.toDate()?.time ?: System.currentTimeMillis()
      trustedServerOffsetMs = serverNow - SystemClock.elapsedRealtime()
      serverNow
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      currentServerNowMillis()
    }
  }

  fun observeUserProfile(uid: String): Flow<Result<UserProfile?>> =
    callbackFlow {
      val path = "$COLLECTION_USER_PROFILES/$uid"
      val listener =
        db.collection(COLLECTION_USER_PROFILES)
          .document(uid)
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              handleFirestoreError(error, OperationType.GET, path)
              trySend(Result.failure(error))
              return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
              trySend(Result.success(null))
              return@addSnapshotListener
            }
            if (!snapshot.metadata.hasPendingWrites()) {
              snapshot.getTimestamp("updatedAt")?.toDate()?.time?.let { serverUpdatedMs ->
                if (trustedServerOffsetMs == null) {
                  trustedServerOffsetMs = serverUpdatedMs - SystemClock.elapsedRealtime()
                }
              }
            }
            val profile =
              snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
            trySend(Result.success(profile))
          }
      awaitClose { listener.remove() }
    }.catch { throwable ->
      val fsEx =
        generateSequence(throwable) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
      if (throwable is CancellationException && fsEx == null) {
        throw throwable
      }
      emit(Result.failure(fsEx ?: throwable))
    }

  suspend fun redeemActivationCode(
    uid: String,
    email: String,
    profileName: String,
    rawCode: String,
  ): Result<UserProfile> {
    val normalizedCode = normalizeCode(rawCode)
    val cleanProfileName = profileName.trim()
    if (cleanProfileName.length !in 2..40) {
      return Result.failure(
        IllegalArgumentException("Le nom du profil doit contenir entre 2 et 40 caractères.")
      )
    }
    if (!CODE_REGEX.matches(normalizedCode)) {
      return Result.failure(
        IllegalArgumentException("Code d'activation invalide.")
      )
    }

    val deviceId = getDeviceIdentifier()
    val serverNow = syncServerTime(uid)
    val codeRef = db.collection(COLLECTION_ACTIVATION_CODES).document(normalizedCode)
    val profileRef = db.collection(COLLECTION_USER_PROFILES).document(uid)

    return try {
      val preCheck = codeRef.get(Source.SERVER).await()
      if (!preCheck.exists()) {
        return Result.failure(
          IllegalStateException("Code d'activation introuvable. Vérifiez la saisie.")
        )
      }
      val existingCode =
        preCheck.toObject(
          ActivationCode::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: return Result.failure(IllegalStateException("Code illisible."))

      when (existingCode.parsedStatus()) {
        ActivationCodeStatus.REDEEMED ->
          return Result.failure(IllegalStateException("Ce code d'activation a déjà été utilisé."))
        ActivationCodeStatus.REVOKED ->
          return Result.failure(IllegalStateException("Ce code d'activation a été révoqué."))
        ActivationCodeStatus.AVAILABLE -> Unit
      }

      if (existingCode.durationMillis <= 0L) {
        return Result.failure(IllegalStateException("Durée du code invalide."))
      }

      val expiresAtMillis = serverNow + existingCode.durationMillis

      db.runTransaction { transaction ->
        val txCodeSnap = transaction.get(codeRef)
        if (!txCodeSnap.exists()) {
          throw IllegalStateException("Code d'activation introuvable.")
        }
        val txStatus = txCodeSnap.getString("status").orEmpty()
        if (txStatus != ActivationCodeStatus.AVAILABLE.name) {
          throw IllegalStateException(
            if (txStatus == ActivationCodeStatus.REVOKED.name) {
              "Ce code d'activation a été révoqué."
            } else {
              "Ce code d'activation a déjà été utilisé."
            }
          )
        }
        val txDuration = (txCodeSnap.get("durationMillis") as? Number)?.toLong() ?: 0L
        val txPlanLabel = txCodeSnap.getString("planLabel").orEmpty()
        if (txDuration <= 0L || txPlanLabel.isBlank()) {
          throw IllegalStateException("Ce code d'activation n'est pas valide.")
        }
        val txExpires = serverNow + txDuration

        transaction.update(
          codeRef,
          mapOf(
            "status" to ActivationCodeStatus.REDEEMED.name,
            "redeemedByUid" to uid,
            "redeemedProfileName" to cleanProfileName,
            "redeemedDeviceId" to deviceId,
            "redeemedAt" to FieldValue.serverTimestamp(),
            "expiresAtMillis" to txExpires,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.set(
          profileRef,
          mapOf(
            "userId" to uid,
            "profileName" to cleanProfileName,
            "email" to email.trim().take(160),
            "activeCode" to normalizedCode,
            "planLabel" to txPlanLabel,
            "durationMillis" to txDuration,
            "deviceId" to deviceId,
            "revoked" to false,
            "activatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "expiresAtMillis" to txExpires,
          ),
        )
      }.await()

      val createdProfile =
        UserProfile(
          userId = uid,
          profileName = cleanProfileName,
          email = email.trim().take(160),
          activeCode = normalizedCode,
          planLabel = existingCode.planLabel,
          durationMillis = existingCode.durationMillis,
          deviceId = deviceId,
          revoked = false,
          expiresAtMillis = expiresAtMillis,
        )
      Result.success(createdProfile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_ACTIVATION_CODES/$normalizedCode")
      Result.failure(e)
    }
  }

  fun observeAllActivationCodes(): Flow<Result<List<ActivationCode>>> =
    callbackFlow {
      val registration =
        db.collection(COLLECTION_ACTIVATION_CODES)
          .orderBy("createdAt", Query.Direction.DESCENDING)
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              handleFirestoreError(error, OperationType.LIST, COLLECTION_ACTIVATION_CODES)
              trySend(Result.failure(error))
              return@addSnapshotListener
            }
            val list =
              snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(
                  ActivationCode::class.java,
                  DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
              }.orEmpty()
            trySend(Result.success(list))
          }
      awaitClose { registration.remove() }
    }.catch { throwable ->
      val fsEx =
        generateSequence(throwable) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
      if (throwable is CancellationException && fsEx == null) {
        throw throwable
      }
      emit(Result.failure(fsEx ?: throwable))
    }

  fun observeAllUserProfiles(): Flow<Result<List<UserProfile>>> =
    callbackFlow {
      val registration =
        db.collection(COLLECTION_USER_PROFILES)
          .orderBy("updatedAt", Query.Direction.DESCENDING)
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              handleFirestoreError(error, OperationType.LIST, COLLECTION_USER_PROFILES)
              trySend(Result.failure(error))
              return@addSnapshotListener
            }
            val list =
              snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(
                  UserProfile::class.java,
                  DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
              }.orEmpty()
            trySend(Result.success(list))
          }
      awaitClose { registration.remove() }
    }.catch { throwable ->
      val fsEx =
        generateSequence(throwable) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
      if (throwable is CancellationException && fsEx == null) {
        throw throwable
      }
      emit(Result.failure(fsEx ?: throwable))
    }

  suspend fun createActivationCode(
    adminUid: String,
    plan: SubscriptionPlan,
    customCode: String? = null,
  ): Result<ActivationCode> {
    val codeId =
      if (!customCode.isNullOrBlank()) {
        normalizeCode(customCode)
      } else {
        generateUniqueCode(plan)
      }
    if (!CODE_REGEX.matches(codeId)) {
      return Result.failure(
        IllegalArgumentException(
          "Code invalide : utilisez 6 à 32 caractères (A-Z, 0-9, tiret)."
        )
      )
    }

    val path = "$COLLECTION_ACTIVATION_CODES/$codeId"
    return try {
      val docRef = db.collection(COLLECTION_ACTIVATION_CODES).document(codeId)
      val existing = docRef.get().await()
      if (existing.exists()) {
        return Result.failure(IllegalStateException("Ce code existe déjà ($codeId)."))
      }

      docRef.set(
        mapOf(
          "code" to codeId,
          "planLabel" to plan.label,
          "durationMillis" to plan.durationMillis,
          "status" to ActivationCodeStatus.AVAILABLE.name,
          "createdByUid" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
          "redeemedByUid" to "",
          "redeemedProfileName" to "",
          "redeemedDeviceId" to "",
          "expiresAtMillis" to 0L,
        )
      ).await()

      Result.success(
        ActivationCode(
          code = codeId,
          planLabel = plan.label,
          durationMillis = plan.durationMillis,
          status = ActivationCodeStatus.AVAILABLE.name,
          createdByUid = adminUid,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun revokeCodeAndProfile(code: ActivationCode): Result<Unit> {
    val path = "$COLLECTION_ACTIVATION_CODES/${code.code}"
    return try {
      val batch = db.batch()
      val codeRef = db.collection(COLLECTION_ACTIVATION_CODES).document(code.code)
      batch.update(
        codeRef,
        mapOf(
          "status" to ActivationCodeStatus.REVOKED.name,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      if (code.redeemedByUid.isNotBlank()) {
        val profileRef = db.collection(COLLECTION_USER_PROFILES).document(code.redeemedByUid)
        batch.update(
          profileRef,
          mapOf(
            "revoked" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteActivationCode(codeId: String): Result<Unit> {
    val path = "$COLLECTION_ACTIVATION_CODES/$codeId"
    return try {
      db.collection(COLLECTION_ACTIVATION_CODES).document(codeId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun setUserRevoked(userId: String, revoked: Boolean): Result<Unit> {
    val path = "$COLLECTION_USER_PROFILES/$userId"
    return try {
      db.collection(COLLECTION_USER_PROFILES)
        .document(userId)
        .update(
          mapOf(
            "revoked" to revoked,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun extendUserSubscription(
    profile: UserProfile,
    plan: SubscriptionPlan,
    adminUid: String,
  ): Result<Unit> {
    val path = "$COLLECTION_USER_PROFILES/${profile.userId}"
    return try {
      val serverNow = syncServerTime(adminUid)
      val baseTime = maxOf(profile.expiresAtMillis, serverNow)
      val newExpires = baseTime + plan.durationMillis
      db.collection(COLLECTION_USER_PROFILES)
        .document(profile.userId)
        .update(
          mapOf(
            "planLabel" to plan.label,
            "durationMillis" to plan.durationMillis,
            "expiresAtMillis" to newExpires,
            "revoked" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  private fun generateUniqueCode(plan: SubscriptionPlan): String {
    val part1 = buildString(4) {
      repeat(4) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }
    val part2 = buildString(4) {
      repeat(4) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }
    return "RIFI-${plan.codePrefix}-$part1-$part2"
  }

  private fun translateAuthError(e: Exception): String {
    val code = (e as? FirebaseAuthException)?.errorCode
    return when (code) {
      "ERROR_INVALID_EMAIL" -> "Adresse e-mail invalide."
      "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
        "Identifiant ou mot de passe incorrect. Si c'est votre première connexion, utilisez « Créer un compte »."
      "ERROR_USER_NOT_FOUND" ->
        "Aucun compte trouvé avec cet e-mail. Passez à l'onglet « Créer un compte »."
      "ERROR_EMAIL_ALREADY_IN_USE" ->
        "Un compte existe déjà avec cet e-mail. Connectez-vous."
      "ERROR_WEAK_PASSWORD" ->
        "Le mot de passe doit contenir au moins 6 caractères."
      "ERROR_TOO_MANY_REQUESTS" ->
        "Trop de tentatives. Patientez quelques instants."
      "ERROR_NETWORK_REQUEST_FAILED" ->
        "Connexion réseau indisponible."
      else -> e.localizedMessage ?: "Erreur d'authentification."
    }
  }

  companion object {
    const val ADMIN_USERNAME = "bigdady"
    const val BOOTSTRAPPED_ADMIN_EMAIL = "sheriforouyo@gmail.com"
    const val COLLECTION_ADMINS = "admins"
    const val COLLECTION_TIME_SYNC = "time_sync"
    const val COLLECTION_ACTIVATION_CODES = "activation_codes"
    const val COLLECTION_USER_PROFILES = "user_profiles"

    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val CODE_REGEX = Regex("^[A-Z0-9_-]{6,32}$")

    fun normalizeCode(raw: String): String =
      raw.trim().uppercase(Locale.ROOT).replace(" ", "")
  }
}
