package com.prod.singles_date.repository

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.prod.singles_date.data.LocalPreferences
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mediaRepository: MediaRepository = MediaRepository(),
) {
    private companion object {
        private const val TAG = "HoghtAuth"
    }

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun signUp(name: String, email: String, password: String, city: String = "") {
        if (city.isNotBlank()) require(AppCity.isValid(city)) { "Invalid city" }
        Log.d(TAG, "signUp(email=$email city=$city)")
        val cred = auth.createUserWithEmailAndPassword(email, password).await()
        val u = cred.user ?: error("Firebase user missing after signup")

        val profile = User(
            uid = u.uid,
            name = name,
            email = u.email ?: email,
            city = city,
            referralCode = LocalPreferences.referralCodeForUid(u.uid),
            createdAt = System.currentTimeMillis(),
        )

        db.collection("users")
            .document(u.uid)
            .set(profile)
            .await()
        Log.d(TAG, "signUp success uid=${u.uid}")
    }

    suspend fun logIn(email: String, password: String) {
        Log.d(TAG, "logIn(email=$email)")
        auth.signInWithEmailAndPassword(email, password).await()
        Log.d(TAG, "logIn success uid=${auth.currentUser?.uid}")
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val u = result.user ?: error("Firebase user missing after Google sign-in")

        val isNewUser = result.additionalUserInfo?.isNewUser == true
        if (isNewUser) {
            val email = u.email.orEmpty()
            val profile = User(
                uid = u.uid,
                name = u.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore('@').ifBlank { "You" },
                email = email,
                photoUrl = u.photoUrl?.toString().orEmpty(),
                referralCode = LocalPreferences.referralCodeForUid(u.uid),
                createdAt = System.currentTimeMillis(),
            )
            db.collection("users")
                .document(u.uid)
                .set(profile)
                .await()
        } else {
            ensureReferralCode(u.uid)
        }
        Log.d(TAG, "signInWithGoogle success uid=${u.uid} new=$isNewUser")
    }

    suspend fun sendPasswordReset(email: String) {
        Log.d(TAG, "sendPasswordReset(email=$email)")
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun updateNotificationPrefs(
        uid: String,
        notifyFeels: Boolean,
        notifyComments: Boolean,
        notifyPrompts: Boolean,
    ) {
        if (uid.isBlank()) return
        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "notifyFeels" to notifyFeels,
                    "notifyComments" to notifyComments,
                    "notifyPrompts" to notifyPrompts,
                    "uid" to uid,
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun syncLocalNotificationPrefs(uid: String, prefs: LocalPreferences) {
        if (uid.isBlank()) return
        updateNotificationPrefs(
            uid = uid,
            notifyFeels = prefs.getNotifyFeels(),
            notifyComments = prefs.getNotifyComments(),
            notifyPrompts = prefs.getNotifyPrompts(),
        )
    }

    suspend fun registerFcmToken(uid: String) {
        if (uid.isBlank()) return
        val token = FirebaseMessaging.getInstance().token.await()
        db.collection("users")
            .document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .await()
        Log.d(TAG, "registerFcmToken success uid=$uid")
    }

    fun logOut() {
        auth.signOut()
    }

    suspend fun deleteAccount(password: String? = null, googleIdToken: String? = null) {
        val user = auth.currentUser ?: error("Not signed in")
        val uid = user.uid
        Log.d(TAG, "deleteAccount uid=$uid")

        when {
            !googleIdToken.isNullOrBlank() -> {
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                user.reauthenticate(credential).await()
            }
            !password.isNullOrBlank() && !user.email.isNullOrBlank() -> {
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
            }
        }

        deleteUserData(uid)
        mediaRepository.deleteProfilePhoto(uid)
        user.delete().await()
        Log.d(TAG, "deleteAccount success uid=$uid")
    }

    private suspend fun deleteUserData(uid: String) {
        val thoughts = db.collection("thoughts")
            .whereEqualTo("authorId", uid)
            .get()
            .await()
        for (doc in thoughts.documents) {
            deleteThoughtWithSubcollections(doc.reference)
        }

        deleteCollectionGroupByField("comments", "userId", uid)
        deleteCollectionGroupByField("feels", "uid", uid)

        val userRef = db.collection("users").document(uid)
        for (sub in listOf("blocked", "hidden", "referrals", "saved")) {
            deleteSubcollection(userRef.collection(sub))
        }
        userRef.delete().await()
    }

    private suspend fun deleteThoughtWithSubcollections(
        thoughtRef: com.google.firebase.firestore.DocumentReference,
    ) {
        for (sub in listOf("comments", "feels")) {
            deleteSubcollection(thoughtRef.collection(sub))
        }
        thoughtRef.delete().await()
    }

    private suspend fun deleteCollectionGroupByField(
        collectionId: String,
        field: String,
        value: String,
    ) {
        var snap = db.collectionGroup(collectionId)
            .whereEqualTo(field, value)
            .limit(500)
            .get()
            .await()
        while (snap.documents.isNotEmpty()) {
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (snap.documents.size < 500) break
            snap = db.collectionGroup(collectionId)
                .whereEqualTo(field, value)
                .limit(500)
                .get()
                .await()
        }
    }

    private suspend fun deleteSubcollection(collection: com.google.firebase.firestore.CollectionReference) {
        var snap = collection.limit(500).get().await()
        while (snap.documents.isNotEmpty()) {
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (snap.documents.size < 500) break
            snap = collection.limit(500).get().await()
        }
    }

    suspend fun updateUserCity(uid: String, city: String) {
        require(AppCity.isValid(city)) { "Invalid city" }
        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "city" to city,
                    "uid" to uid,
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun updateUserLocality(uid: String, locality: String) {
        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "locality" to locality,
                    "uid" to uid,
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun updateUserOnboarding(uid: String, city: String, locality: String) {
        require(AppCity.isValid(city)) { "Invalid city" }
        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "city" to city,
                    "locality" to locality,
                    "uid" to uid,
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun updateUserName(uid: String, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank() && trimmed.length <= 60) { "Invalid name" }
        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "name" to trimmed,
                    "uid" to uid,
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun updateUserPhotoUrl(uid: String, photoUrl: String) {
        if (uid.isBlank()) return
        require(photoUrl.length <= 2048) { "Invalid photo URL" }
        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "photoUrl" to photoUrl,
                    "uid" to uid,
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun ensureUserProfile(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        if (uid.isBlank()) return
        val snap = db.collection("users").document(uid).get().await()
        if (snap.exists()) return
        val email = firebaseUser.email.orEmpty()
        val profile = User(
            uid = uid,
            name = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore('@').ifBlank { "You" },
            email = email,
            photoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
            referralCode = LocalPreferences.referralCodeForUid(uid),
            createdAt = System.currentTimeMillis(),
        )
        db.collection("users").document(uid).set(profile).await()
        Log.d(TAG, "ensureUserProfile created uid=$uid")
    }

    suspend fun ensureReferralCode(uid: String) {
        if (uid.isBlank()) return
        val snap = db.collection("users").document(uid).get().await()
        val existing = snap.getString("referralCode").orEmpty()
        if (existing.isNotBlank()) return
        db.collection("users")
            .document(uid)
            .set(
                mapOf("referralCode" to LocalPreferences.referralCodeForUid(uid)),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun applyReferral(newUserUid: String, referralCode: String) {
        val code = referralCode.trim().lowercase()
        if (code.isBlank() || newUserUid.isBlank()) return

        val userSnap = db.collection("users").document(newUserUid).get().await()
        if (userSnap.getString("referredBy").orEmpty().isNotBlank()) return

        val referrerQuery = db.collection("users")
            .whereEqualTo("referralCode", code)
            .limit(1)
            .get()
            .await()

        val referrerDoc = referrerQuery.documents.firstOrNull() ?: return
        val referrerUid = referrerDoc.id
        if (referrerUid == newUserUid) return

        db.collection("users")
            .document(newUserUid)
            .set(mapOf("referredBy" to referrerUid), SetOptions.merge())
            .await()

        db.collection("users")
            .document(referrerUid)
            .collection("referrals")
            .document(newUserUid)
            .set(mapOf("createdAt" to System.currentTimeMillis()))
            .await()
    }
}
