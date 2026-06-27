package com.prod.singles_date.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.prod.singles_date.ui.util.findActivity
import com.prod.singles_date.ui.util.humanizeLegacyGoogleSignInError
import com.prod.singles_date.ui.util.resolveGoogleWebClientId
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Opens the full Google account picker (all accounts on the device + add account).
 * Avoids Credential Manager One Tap, which only surfaces a single suggested account.
 */
@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    onSignIn: (idToken: String) -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onSignInState = rememberUpdatedState(onSignIn)
    val onErrorState = rememberUpdatedState(onError)
    val activity = context.findActivity()
    val webClientId = resolveGoogleWebClientId(context)

    val legacySignInClient = remember(activity, webClientId) {
        if (activity == null || webClientId.isNullOrBlank()) return@remember null
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(activity, options)
    }

    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data: Intent = result.data ?: return@rememberLauncherForActivityResult
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            if (!token.isNullOrBlank()) {
                onSignInState.value(token)
            } else {
                onErrorState.value("Google did not return a sign-in token. Try again or use email sign-in.")
            }
        } catch (e: ApiException) {
            val message = humanizeLegacyGoogleSignInError(e.statusCode)
            if (message.isNotBlank()) onErrorState.value(message)
        }
    }

    fun launchAccountPicker() {
        val client = legacySignInClient
        if (client == null) {
            onError("Google sign-in is unavailable on this screen. Try again.")
            return
        }
        scope.launch {
            runCatching { client.signOut().await() }
            legacyLauncher.launch(client.signInIntent)
        }
    }

    OutlinedButton(
        onClick = {
            if (activity == null) {
                onError("Google sign-in is unavailable on this screen. Try again.")
                return@OutlinedButton
            }
            if (webClientId.isNullOrBlank()) {
                onError("Google sign-in config is missing. Reinstall the latest app build.")
                return@OutlinedButton
            }
            launchAccountPicker()
        },
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
    }
}
