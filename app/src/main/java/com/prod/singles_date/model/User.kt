package com.prod.singles_date.model

import androidx.annotation.Keep
import com.google.firebase.auth.FirebaseUser
import com.prod.singles_date.data.LocalPreferences

@Keep
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val locality: String = "",
    val referralCode: String = "",
    val referredBy: String = "",
    val referralCount: Int = 0,
    val fcmToken: String = "",
    val postStreak: Int = 0,
    val lastPostDate: String = "",
    val voiceScore: Int = 0,
    val badges: List<String> = emptyList(),
    val totalFeelsReceived: Int = 0,
    val totalCommentsWritten: Int = 0,
    val isPremium: Boolean = false,
    val notifyFeels: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyPrompts: Boolean = true,
    val notifyMessages: Boolean = true,
    val photoUrl: String = "",
    val savedThoughtIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /** Shown instantly while Firestore profile loads or if the doc is missing. */
        fun fromFirebase(
            firebaseUser: FirebaseUser,
            firestore: User? = null,
            prefs: LocalPreferences? = null,
        ): User {
            if (firestore != null && firestore.uid.isNotBlank()) return firestore
            val email = firebaseUser.email.orEmpty()
            return User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore('@').ifBlank { "You" },
                email = email,
                city = prefs?.getGuestCity().orEmpty(),
                locality = prefs?.getGuestLocality().orEmpty(),
                photoUrl = firestore?.photoUrl?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.photoUrl?.toString().orEmpty(),
                referralCode = firestore?.referralCode?.takeIf { it.isNotBlank() }
                    ?: LocalPreferences.referralCodeForUid(firebaseUser.uid),
            )
        }
    }
}
