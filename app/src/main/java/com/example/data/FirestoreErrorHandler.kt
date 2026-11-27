package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

enum class OperationType {
  CREATE,
  UPDATE,
  DELETE,
  GET,
  LIST,
  WRITE,
}

fun handleFirestoreError(
  error: Exception,
  operationType: OperationType,
  path: String?,
) {
  val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
  val user = auth?.currentUser
  val payload =
    JSONObject().apply {
      put("error", error.message ?: error.javaClass.simpleName)
      put("operationType", operationType.name.lowercase())
      put("path", path ?: JSONObject.NULL)
      put(
        "authInfo",
        JSONObject().apply {
          put("userId", user?.uid ?: JSONObject.NULL)
          put("email", user?.email ?: JSONObject.NULL)
          put("emailVerified", user?.isEmailVerified ?: false)
          put(
            "providerIds",
            user?.providerData?.mapNotNull { it.providerId }?.joinToString(",") ?: "",
          )
        },
      )
    }
  Log.e("FirestoreError", payload.toString(), error)
}
