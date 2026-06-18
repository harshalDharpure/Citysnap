package com.prod.singles_date.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.prod.singles_date.model.Thought
import com.prod.singles_date.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private companion object {
        private const val TAG = "InnerCircleFirestore"
    }

    fun userProfileFlow(uid: String): Flow<User?> = callbackFlow {
        val reg = db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "userProfileFlow($uid) listener error", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java)?.let { profile ->
                    if (profile.uid.isBlank()) profile.copy(uid = uid) else profile
                })
            }
        awaitClose { reg.remove() }
    }

    fun myThoughtsFlow(uid: String): Flow<List<Thought>> = callbackFlow {
        val reg = db.collection("thoughts")
            .whereEqualTo("authorId", uid)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "myThoughtsFlow($uid) listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val t = doc.toObject(Thought::class.java) ?: return@mapNotNull null
                    if (t.id.isNotBlank()) t else t.copy(id = doc.id)
                }.orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun referralCountFlow(uid: String): Flow<Int> = callbackFlow {
        val reg = db.collection("users")
            .document(uid)
            .collection("referrals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "referralCountFlow($uid) listener error", error)
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { reg.remove() }
    }
}

