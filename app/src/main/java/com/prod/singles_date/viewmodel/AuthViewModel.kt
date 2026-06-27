package com.prod.singles_date.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException
import com.prod.singles_date.model.User
import com.prod.singles_date.model.AppCity
import android.content.Context
import android.net.Uri
import com.google.firebase.analytics.FirebaseAnalytics
import com.prod.singles_date.repository.AuthRepository
import com.prod.singles_date.repository.MediaRepository
import com.prod.singles_date.repository.ProfileRepository
import com.prod.singles_date.util.AnalyticsEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(
        val firebaseUser: FirebaseUser,
        val userProfile: User? = null
    ) : SessionState
}

/**
 * Represents one-time UI events like successful login/signup.
 */
sealed interface AuthUiEvent {
    data object AuthSuccess : AuthUiEvent
}

/**
 * Firebase Auth session + profile. UI observes [session] and triggers actions via methods.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val mediaRepository: MediaRepository,
    private val analytics: FirebaseAnalytics,
) : ViewModel() {

    private val _authMessage = MutableStateFlow("")
    val authMessage: StateFlow<String> = _authMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()
    // Back-compat alias for UI that reads `busy`.
    val busy: StateFlow<Boolean> = isBusy

    private val _events = Channel<AuthUiEvent>()
    val events = _events.receiveAsFlow()

    /**
     * Combined session state: handles both Firebase Auth and the Firestore Profile.
     */
    val session: StateFlow<SessionState> = authRepository.authState()
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(SessionState.LoggedOut)
            } else {
                profileRepository.userProfileFlow(firebaseUser.uid)
                    .map { profile -> SessionState.LoggedIn(firebaseUser, profile) }
                    // If profile fetch fails, we are still logged in but with no profile data.
                    .catch { emit(SessionState.LoggedIn(firebaseUser, null)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionState.Loading
        )

    /** Convenience stream for current FirebaseAuth user. */
    val firebaseUser: StateFlow<FirebaseUser?> = session
        .map { (it as? SessionState.LoggedIn)?.firebaseUser }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Convenience stream for Firestore `users/{uid}` profile (may be null while loading/denied). */
    val currentUser: StateFlow<User?> = session
        .map { (it as? SessionState.LoggedIn)?.userProfile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isLoggedIn: StateFlow<Boolean> = session
        .map { it is SessionState.LoggedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    fun clearMessage() {
        _authMessage.value = ""
    }

    /** Surface an error raised by the UI-side Google sign-in flow. */
    fun reportAuthError(message: String) {
        _authMessage.value = message
    }

    private var pendingReferralCode: String? = null

    fun setPendingReferralCode(code: String?) {
        pendingReferralCode = code?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            performAuthAction(analyticsMethod = "google") {
                authRepository.signInWithGoogle(idToken)
                applyPendingReferralIfAny()
            }
        }
    }

    fun logOut() {
        authRepository.logOut()
    }

    fun deleteAccount(password: String? = null, googleIdToken: String? = null, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isBusy.value = true
            _authMessage.value = ""
            try {
                authRepository.deleteAccount(password, googleIdToken)
                onDone(true, "")
            } catch (t: Throwable) {
                val msg = when ((t as? FirebaseAuthException)?.errorCode) {
                    "ERROR_REQUIRES_RECENT_LOGIN" ->
                        "For security, please enter your password or sign in with Google again to delete your account."
                    else -> humanizeDeleteAccountError(t)
                }
                _authMessage.value = msg
                onDone(false, msg)
            } finally {
                _isBusy.value = false
            }
        }
    }

    /** Register this device for push notifications once the user is known. */
    fun registerPushToken(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            runCatching { authRepository.registerFcmToken(uid) }
        }
    }

    fun updateNotificationPrefs(
        notifyFeels: Boolean,
        notifyComments: Boolean,
        notifyPrompts: Boolean,
        notifyMessages: Boolean,
    ) {
        val uid = firebaseUser.value?.uid ?: authRepository.currentUser()?.uid ?: return
        viewModelScope.launch {
            runCatching {
                authRepository.updateNotificationPrefs(
                    uid,
                    notifyFeels,
                    notifyComments,
                    notifyPrompts,
                    notifyMessages,
                )
            }
        }
    }

    fun signUp(name: String, email: String, password: String, city: String = "") {
        val n = name.trim()
        val e = email.trim()
        val p = password.trim()
        val c = city.trim()

        if (n.isEmpty() || e.isEmpty() || p.isEmpty()) {
            _authMessage.value = "Please fill in every field."
            return
        }
        if (c.isNotBlank() && !AppCity.isValid(c)) {
            _authMessage.value = "Please pick your city."
            return
        }

        viewModelScope.launch {
            performAuthAction(analyticsMethod = "email", isSignUp = true) {
                authRepository.signUp(name = n, email = e, password = p, city = c)
                applyPendingReferralIfAny()
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val e = email.trim()
        if (e.isEmpty()) {
            _authMessage.value = "Enter your email first, then tap reset."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _authMessage.value = ""
            try {
                authRepository.sendPasswordReset(e)
                _authMessage.value = "Password reset link sent to $e."
            } catch (t: Throwable) {
                _authMessage.value = authErrorMessage(t)
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun logIn(email: String, password: String) {
        val e = email.trim()
        val p = password.trim()

        if (e.isEmpty() || p.isEmpty()) {
            _authMessage.value = "Enter email and password."
            return
        }

        viewModelScope.launch {
            performAuthAction(analyticsMethod = "email") {
                authRepository.logIn(email = e, password = p)
                applyPendingReferralIfAny()
            }
        }
    }

    /** Apply a pending invite code stored from a deep link. */
    fun applyPendingReferral(code: String) {
        setPendingReferralCode(code)
        viewModelScope.launch {
            applyPendingReferralIfAny()
        }
    }

    private suspend fun applyPendingReferralIfAny() {
        val code = pendingReferralCode ?: return
        val uid = authRepository.currentUser()?.uid ?: return
        runCatching {
            authRepository.applyReferral(uid, code)
            pendingReferralCode = null
        }
    }

    /** Creates a Firestore profile if missing (e.g. legacy Google accounts). */
    fun ensureUserProfile() {
        val firebase = firebaseUser.value ?: authRepository.currentUser() ?: return
        viewModelScope.launch {
            runCatching { authRepository.ensureUserProfile(firebase) }
        }
    }

    /** Persist city and locality for a signed-in user. */
    fun saveOnboarding(city: String, locality: String, onDone: (Boolean) -> Unit = {}) {
        val uid = firebaseUser.value?.uid ?: authRepository.currentUser()?.uid
        if (uid.isNullOrBlank()) {
            onDone(true)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching {
                authRepository.updateUserOnboarding(uid, city.trim(), locality.trim())
            }
            _isBusy.value = false
            if (result.isSuccess) {
                onDone(true)
            } else {
                _authMessage.value = "Couldn't save your city. Check your connection and try again."
                onDone(false)
            }
        }
    }

    fun updateUserName(name: String, onDone: () -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || trimmed.length > 60) return
        val uid = firebaseUser.value?.uid ?: authRepository.currentUser()?.uid ?: return
        viewModelScope.launch {
            _isBusy.value = true
            runCatching { authRepository.updateUserName(uid, trimmed) }
            _isBusy.value = false
            onDone()
        }
    }

    fun updateProfilePhoto(context: Context, imageUri: Uri, onDone: () -> Unit = {}) {
        val uid = firebaseUser.value?.uid ?: authRepository.currentUser()?.uid ?: return
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            _authMessage.value = ""
            runCatching {
                val url = mediaRepository.uploadProfilePhoto(context.applicationContext, uid, imageUri)
                authRepository.updateUserPhotoUrl(uid, url)
            }.onFailure { error ->
                _authMessage.value = humanizePhotoUploadError(error)
            }
            _isUploadingPhoto.value = false
            onDone()
        }
    }

    /** Persist city for a signed-in user (also used when changing city later). */
    fun saveCityForUser(city: String, onDone: () -> Unit = {}) {
        val c = city.trim()
        if (!AppCity.isValid(c)) return
        val uid = firebaseUser.value?.uid ?: authRepository.currentUser()?.uid
        if (uid.isNullOrBlank()) {
            onDone()
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            runCatching { authRepository.updateUserCity(uid, c) }
            _isBusy.value = false
            onDone()
        }
    }

    /**
     * Shared wrapper for login/signup to reduce boilerplate.
     */
    private suspend fun performAuthAction(
        analyticsMethod: String? = null,
        isSignUp: Boolean = false,
        action: suspend () -> Unit,
    ) {
        _isBusy.value = true
        _authMessage.value = ""
        try {
            action()
            analyticsMethod?.let { method ->
                if (isSignUp) {
                    AnalyticsEvents.logSignUp(analytics, method)
                } else {
                    AnalyticsEvents.logLogin(analytics, method)
                }
            }
            _events.send(AuthUiEvent.AuthSuccess)
        } catch (t: Throwable) {
            _authMessage.value = authErrorMessage(t)
        } finally {
            _isBusy.value = false
        }
    }

    private fun authErrorMessage(t: Throwable): String {
        val firebaseCode = (t as? FirebaseAuthException)?.errorCode
        val raw = t.message?.takeIf { it.isNotBlank() }
        
        return when (firebaseCode) {
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_WRONG_PASSWORD" -> "Invalid password."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email."
            "ERROR_INVALID_LOGIN_CREDENTIALS" -> "Invalid email or password."
            "ERROR_INVALID_CREDENTIAL" ->
                "Google sign-in failed. Update the app from Play Store, or use email sign-in."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in is disabled in Firebase Console."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check internet and try again."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use."
            "ERROR_WEAK_PASSWORD" -> "The password is too weak."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "This email is already linked to a different sign-in method. Try email/password instead."
            null -> raw ?: "An unexpected error occurred."
            else -> raw ?: firebaseCode
        }
    }
}

private fun humanizeDeleteAccountError(error: Throwable): String {
    val msg = error.message.orEmpty().lowercase()
    return when {
        msg.contains("permission") ->
            "Couldn't delete your account data. Update the app and try again, or contact support."
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "Couldn't delete your account. Please try again."
    }
}

private fun humanizePhotoUploadError(error: Throwable): String {
    val msg = error.message.orEmpty().lowercase()
    return when {
        msg.contains("network") || msg.contains("unavailable") || msg.contains("timeout") ->
            "No internet connection. Check your network and try again."
        msg.contains("storage") || msg.contains("upload") || msg.contains("object") ->
            "Couldn't upload your photo. Try a smaller image."
        msg.contains("permission") ->
            "Permission denied. Sign out and sign back in, then try again."
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "Couldn't update profile photo. Please try again."
    }
}
