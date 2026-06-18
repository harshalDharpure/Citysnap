package com.prod.singles_date.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.prod.singles_date.ui.util.fetchGoogleIdToken
import com.prod.singles_date.ui.util.resolveGoogleWebClientId
import kotlinx.coroutines.launch

/**
 * "Continue with Google" button. Runs the Credential Manager flow, then hands
 * the resulting ID token back to the caller (which exchanges it for a Firebase
 * session). Surfaces user-friendly errors via [onError].
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

    OutlinedButton(
        onClick = {
            val webClientId = resolveGoogleWebClientId(context)
            if (webClientId.isNullOrBlank()) {
                onError("Google sign-in config is missing. Reinstall the latest app build.")
                return@OutlinedButton
            }
            scope.launch {
                try {
                    val idToken = fetchGoogleIdToken(context, webClientId)
                    onSignIn(idToken)
                } catch (_: GetCredentialCancellationException) {
                    // User dismissed the chooser; no message needed.
                } catch (_: NoCredentialException) {
                    onError(
                        "No Google account available. Add one in Settings → Passwords & accounts, " +
                            "or use an emulator image with Google Play.",
                    )
                } catch (e: GetCredentialException) {
                    onError("Google sign-in failed. Please try again.")
                }
            }
        },
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
    }
}
