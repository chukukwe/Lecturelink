package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object AuthManager {
    private const val TAG = "AuthManager"

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        try {
            Firebase.auth.addAuthStateListener { auth ->
                _currentUser.value = auth.currentUser
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register auth state listener", t)
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        try {
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
                return
            }
            val clientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Throwable) {
                Log.w(TAG, "default_web_client_id not available: ${e.message}")
                onUnauthenticated()
                return
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        Firebase.auth.signInWithCredential(authCredential).await()
                        onAuthSuccess()
                    } else {
                        onUnauthenticated()
                    }
                } catch (t: Throwable) {
                    Log.d(TAG, "Silent auto-sign in not applicable: ${t.message}")
                    onUnauthenticated()
                }
            }
        } catch (t: Throwable) {
            Log.d(TAG, "attemptAutoSignIn skipped: ${t.message}")
            onUnauthenticated()
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        onAuthSuccess: (FirebaseUser) -> Unit,
        onAuthError: (String) -> Unit,
        onCancelled: () -> Unit,
        scope: CoroutineScope
    ) {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Throwable) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    authResult.user?.let(onAuthSuccess) ?: onAuthError("User is null after sign-in")
                } else {
                    onAuthError("Unexpected credential format received.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                onCancelled()
            } catch (t: Throwable) {
                Log.e(TAG, "Google Sign-In error", t)
                val rawMsg = t.localizedMessage ?: "Sign-in failed"
                val friendlyMessage = when {
                    rawMsg.contains("no provider dependencies found", ignoreCase = true) ||
                    rawMsg.contains("GetCredentialProviderConfigurationException", ignoreCase = true) -> {
                        "Google Play Services is outdated or unconfigured in BlueStacks/this emulator.\n\nTo fix:\n1. Open Google Play Store inside BlueStacks and sign into a Google account.\n2. Ensure Google Play Services is updated, or test on a physical Android phone with Google Play."
                    }
                    rawMsg.contains("NoCredentialException", ignoreCase = true) -> {
                        "No Google account found on this device. Please sign in to your Google Account in BlueStacks / Android Settings > Accounts first."
                    }
                    else -> rawMsg
                }
                onAuthError(friendlyMessage)
            }
        }
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onComplete: () -> Unit
    ) {
        try {
            Firebase.auth.signOut()
        } catch (t: Throwable) {
            Log.e(TAG, "Error signing out of Firebase Auth", t)
        }
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to clear credential state", t)
            } finally {
                onComplete()
            }
        }
    }
}
