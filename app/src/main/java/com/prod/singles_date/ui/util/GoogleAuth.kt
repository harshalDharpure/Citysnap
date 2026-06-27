package com.prod.singles_date.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.prod.singles_date.BuildConfig
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
    val fromGenerated = runCatching {
        context.getString(R.string.default_web_client_id)
    }.getOrNull()?.takeIf { it.isNotBlank() }
    if (!fromGenerated.isNullOrBlank()) return fromGenerated
    return BuildConfig.GOOGLE_WEB_CLIENT_ID.takeIf { it.isNotBlank() }
}

fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Launch the Credential Manager "Sign in with Google" flow and return the
 * Google ID token, which is then exchanged for a Firebase credential.
 */
suspend fun fetchGoogleIdToken(context: Context, webClientId: String): String {
    val activity = context.findActivity()
        ?: error("Google sign-in requires an Activity context.")
    val credentialManager = CredentialManager.create(activity)

    try {
        return requestIdToken(
            activity,
            credentialManager,
            googleIdRequest(webClientId, filterAuthorized = true),
        )
    } catch (e: GetCredentialCancellationException) {
        throw e
    } catch (_: NoCredentialException) {
        // No account has signed in to this app before — try all on-device accounts.
    }

    try {
        return requestIdToken(
            activity,
            credentialManager,
            googleIdRequest(webClientId, filterAuthorized = false),
        )
    } catch (e: GetCredentialCancellationException) {
        throw e
    } catch (_: NoCredentialException) {
        // No on-device Google account — open the full Sign in with Google sheet.
    }

    return requestIdToken(
        activity,
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
    activity: Activity,
    credentialManager: CredentialManager,
    request: GetCredentialRequest,
): String {
    val response = credentialManager.getCredential(activity, request)
    val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
    return googleCredential.idToken
        ?: error("Google did not return a sign-in token. Try again or use email sign-in.")
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

fun humanizeGoogleCredentialError(error: GetCredentialException): String {
    val raw = error.message.orEmpty().lowercase()
    return when {
        error is GetCredentialCancellationException -> ""
        error is NoCredentialException ->
            "No Google account available. Add one in Settings → Passwords & accounts."
        raw.contains("developer") || raw.contains("10:") || raw.contains("invalid") ->
            "Google sign-in configuration error. Update the app from Play Store, or try email sign-in."
        else ->
            error.message?.takeIf { it.isNotBlank() }
                ?: "Google sign-in failed. Check your connection and try again."
    }
}

fun humanizeLegacyGoogleSignInError(statusCode: Int): String {
    return when (statusCode) {
        10 -> "Google sign-in is not configured for this app build. Update from Play Store or use email sign-in."
        7 -> "Network error. Check your connection and try again."
        12501 -> ""
        else -> "Google sign-in failed (code $statusCode). Try again or use email sign-in."
    }
}
