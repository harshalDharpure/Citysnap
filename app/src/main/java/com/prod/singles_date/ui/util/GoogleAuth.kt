package com.prod.singles_date.ui.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.prod.singles_date.R
import java.security.SecureRandom
import java.util.Base64

/**
 * Firebase "Web client" OAuth id from [google-services.json].
 *
 * Must reference [R.string.default_web_client_id] directly so release
 * resource shrinking does not strip it (dynamic getIdentifier lookups are invisible to the shrinker).
 */
fun resolveGoogleWebClientId(context: Context): String? {
    return context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
}

/**
 * Launch the Credential Manager "Sign in with Google" flow and return the
 * Google ID token, which is then exchanged for a Firebase credential.
 *
 * Follows Google's recommended three-step flow:
 * 1. Previously authorized account (returning user)
 * 2. Any Google account on the device (sign-up picker)
 * 3. Full Sign in with Google UI (pick or add an account)
 */
suspend fun fetchGoogleIdToken(context: Context, webClientId: String): String {
    val credentialManager = CredentialManager.create(context)

    try {
        return requestIdToken(
            context,
            credentialManager,
            googleIdRequest(webClientId, filterAuthorized = true),
        )
    } catch (_: NoCredentialException) {
        // No account has signed in to this app before — try all on-device accounts.
    }

    try {
        return requestIdToken(
            context,
            credentialManager,
            googleIdRequest(webClientId, filterAuthorized = false),
        )
    } catch (_: NoCredentialException) {
        // No on-device Google account — open the full Sign in with Google sheet.
    }

    return requestIdToken(
        context,
        credentialManager,
        GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                    .setNonce(secureNonce())
                    .build(),
            )
            .build(),
    )
}

private fun googleIdRequest(webClientId: String, filterAuthorized: Boolean): GetCredentialRequest {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(filterAuthorized)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
        .setNonce(secureNonce())
        .build()
    return GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()
}

private suspend fun requestIdToken(
    context: Context,
    credentialManager: CredentialManager,
    request: GetCredentialRequest,
): String {
    val response = credentialManager.getCredential(context, request)
    val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
    return googleCredential.idToken
}

private fun secureNonce(byteLength: Int = 32): String {
    val bytes = ByteArray(byteLength)
    try {
        SecureRandom.getInstanceStrong().nextBytes(bytes)
    } catch (_: Exception) {
        SecureRandom().nextBytes(bytes)
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
