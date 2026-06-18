package com.prod.singles_date.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.prod.singles_date.model.AppNotification
import com.prod.singles_date.model.CityMood
import com.prod.singles_date.model.Comment
import com.prod.singles_date.model.PostType
import com.prod.singles_date.model.Thought
import com.prod.singles_date.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ThoughtRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mediaRepository: MediaRepository = MediaRepository(),
) {
    private companion object {
        private const val TAG = "HoghtFirestore"
        private const val THOUGHTS_LIMIT = 150L
        private const val PAGE_SIZE = 30
    }

    fun pageSize(): Int = PAGE_SIZE

    /**
     * City-scoped feed. Filters locality/category client-side so Firestore does not
     * require composite indexes (which often fail silently and return an empty feed).
     */
    fun thoughtsFlow(
        city: String,
        locality: String = "",
        category: String = "",
    ): Flow<List<Thought>> = callbackFlow {
        var query: Query = db.collection("thoughts")
        if (city.isNotBlank()) {
            query = query.whereEqualTo("city", city)
        }
        query = query.limit(THOUGHTS_LIMIT * 2)

        val reg = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "thoughtsFlow($city,$locality,$category) error", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                val t = doc.toObject(Thought::class.java) ?: return@mapNotNull null
                if (t.id.isNotBlank()) t else t.copy(id = doc.id)
            }.orEmpty()
                .filter { thought ->
                    (locality.isBlank() || thought.locality == locality) &&
                        (category.isBlank() || thought.category == category)
                }
                .sortedByDescending { it.createdAt }
                .take(THOUGHTS_LIMIT.toInt())
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    fun cityMoodFlow(city: String): Flow<CityMood?> = callbackFlow {
        if (city.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val weekId = currentWeekId()
        val reg = db.collection("city_mood")
            .document(city)
            .collection("weekly")
            .document(weekId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "cityMoodFlow($city) error", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(CityMood::class.java))
            }
        awaitClose { reg.remove() }
    }

    fun dailyPromptFlow(city: String): Flow<String?> = callbackFlow {
        if (city.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val date = todayDateString()
        val reg = db.collection("prompts")
            .document(city)
            .collection("daily")
            .document(date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.getString("text"))
            }
        awaitClose { reg.remove() }
    }

    suspend fun postThought(
        text: String,
        authorId: String,
        authorName: String,
        authorPhotoUrl: String = "",
        city: String,
        locality: String,
        category: String,
        postType: String = PostType.SNAP,
        context: Context,
        imageUris: List<Uri> = emptyList(),
    ) {
        val type = postType.ifBlank { PostType.SNAP }
        require(PostType.isValid(type)) { "Invalid post type" }
        val maxLen = PostType.maxLength(type)
        val trimmedText = text.trim().take(maxLen)
        val cappedUris = if (type == PostType.NOTE) emptyList() else imageUris.take(MediaRepository.MAX_IMAGES_PER_POST)
        if ((trimmedText.isBlank() && cappedUris.isEmpty()) || city.isBlank()) return
        if (type == PostType.NOTE && category.isBlank()) {
            error("Pick a category for Local Notes")
        }

        val doc = db.collection("thoughts").document()
        val imageUrls = if (cappedUris.isNotEmpty()) {
            mediaRepository.uploadThoughtImages(
                context.applicationContext,
                authorId,
                doc.id,
                cappedUris,
            )
        } else {
            emptyList()
        }

        val thought = Thought(
            id = doc.id,
            text = trimmedText,
            feelCount = 0,
            commentCount = 0,
            shareCount = 0,
            authorId = authorId,
            authorName = authorName.trim(),
            authorPhotoUrl = authorPhotoUrl.trim(),
            city = city,
            locality = locality,
            category = category,
            postType = type,
            isSponsored = false,
            sponsorLabel = "",
            sponsorUrl = "",
            imageUrls = imageUrls,
            imageCount = imageUrls.size,
            createdAt = System.currentTimeMillis(),
        )
        doc.set(thought).await()
        updatePostStreak(authorId)
    }

    private suspend fun updatePostStreak(authorId: String) {
        val today = todayDateString()
        val userRef = db.collection("users").document(authorId)
        db.runTransaction { tx ->
            val snap = tx.get(userRef)
            val lastDate = snap.getString("lastPostDate").orEmpty()
            val currentStreak = snap.getLong("postStreak")?.toInt() ?: 0
            val newStreak = when {
                lastDate == today -> currentStreak
                lastDate == yesterdayDateString() -> currentStreak + 1
                else -> 1
            }
            tx.set(
                userRef,
                mapOf("postStreak" to newStreak, "lastPostDate" to today),
                SetOptions.merge(),
            )
        }.await()
    }

    suspend fun toggleSaveThought(uid: String, thoughtId: String, save: Boolean) {
        val ref = db.collection("users").document(uid).collection("saved").document(thoughtId)
        if (save) {
            ref.set(mapOf("createdAt" to System.currentTimeMillis())).await()
        } else {
            ref.delete().await()
        }
    }

    fun savedThoughtIdsFlow(uid: String): Flow<Set<String>> = callbackFlow {
        val reg = db.collection("users").document(uid).collection("saved")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.id }?.toSet().orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateThought(thoughtId: String, newText: String) {
        db.collection("thoughts").document(thoughtId).update("text", newText).await()
    }

    suspend fun deleteThought(thoughtId: String, authorId: String) {
        require(thoughtId.isNotBlank()) { "Thought id required" }
        require(authorId.isNotBlank()) { "Author id required" }
        val ref = db.collection("thoughts").document(thoughtId)
        val snap = ref.get().await()
        if (!snap.exists()) return
        val ownerId = snap.getString("authorId").orEmpty()
        require(ownerId == authorId) { "You can only delete your own posts" }
        mediaRepository.deleteThoughtImages(authorId, thoughtId)
        ref.delete().await()
    }

    suspend fun toggleFeel(thoughtId: String, uid: String): Boolean {
        val thoughtRef = db.collection("thoughts").document(thoughtId)
        val feelRef = thoughtRef.collection("feels").document(uid)
        return db.runTransaction { tx ->
            val existing = tx.get(feelRef)
            if (existing.exists()) {
                tx.delete(feelRef)
                tx.update(thoughtRef, "feelCount", FieldValue.increment(-1))
                false
            } else {
                tx.set(
                    feelRef,
                    mapOf("uid" to uid, "thoughtId" to thoughtId, "createdAt" to System.currentTimeMillis()),
                )
                tx.update(thoughtRef, "feelCount", FieldValue.increment(1))
                true
            }
        }.await()
    }

    suspend fun incrementShareCount(thoughtId: String) {
        if (thoughtId.isBlank()) return
        runCatching {
            db.collection("thoughts").document(thoughtId)
                .update("shareCount", FieldValue.increment(1))
                .await()
        }
    }

    fun feeledThoughtIdsFlow(uid: String): Flow<Set<String>> = callbackFlow {
        val reg = db.collectionGroup("feels")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.getString("thoughtId") }?.toSet().orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun commentsFlow(thoughtId: String): Flow<List<Comment>> = callbackFlow {
        val reg = db.collection("thoughts").document(thoughtId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val c = doc.toObject(Comment::class.java) ?: return@mapNotNull null
                    if (c.id.isNotBlank()) c else c.copy(id = doc.id)
                }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(thoughtId: String, userId: String, userName: String, text: String) {
        val thoughtRef = db.collection("thoughts").document(thoughtId)
        val doc = thoughtRef.collection("comments").document()
        val comment = Comment(
            id = doc.id,
            userId = userId,
            userName = userName,
            text = text,
            createdAt = System.currentTimeMillis(),
        )
        db.runTransaction { tx ->
            tx.set(doc, comment)
            tx.update(thoughtRef, "commentCount", FieldValue.increment(1))
        }.await()
        db.collection("users").document(userId)
            .update("totalCommentsWritten", FieldValue.increment(1))
            .await()
    }

    suspend fun reportThought(thoughtId: String, reporterUid: String, reason: String) {
        db.collection("reports").add(
            mapOf(
                "thoughtId" to thoughtId,
                "reporterUid" to reporterUid,
                "reason" to reason,
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
        hideThought(thoughtId, reporterUid)
    }

    suspend fun blockUser(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank() || blockerUid == blockedUid) return
        db.collection("users").document(blockerUid).collection("blocked")
            .document(blockedUid)
            .set(mapOf("createdAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun unblockUser(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        db.collection("users").document(blockerUid).collection("blocked")
            .document(blockedUid)
            .delete()
            .await()
    }

    suspend fun getUserDisplayName(uid: String): String {
        if (uid.isBlank()) return "User"
        val snap = db.collection("users").document(uid).get().await()
        return snap.getString("name")?.takeIf { it.isNotBlank() } ?: "User"
    }

    fun thoughtFlow(thoughtId: String): Flow<Thought?> = callbackFlow {
        if (thoughtId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = db.collection("thoughts").document(thoughtId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val thought = snapshot.toObject(Thought::class.java)?.copy(id = snapshot.id)
                trySend(thought)
            }
        awaitClose { reg.remove() }
    }

    suspend fun reportComment(
        thoughtId: String,
        commentId: String,
        reporterUid: String,
        reason: String,
    ) {
        db.collection("reports").add(
            mapOf(
                "thoughtId" to thoughtId,
                "commentId" to commentId,
                "reporterUid" to reporterUid,
                "reason" to reason,
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    private suspend fun hideThought(thoughtId: String, uid: String) {
        db.collection("users").document(uid).collection("hidden")
            .document(thoughtId)
            .set(mapOf("createdAt" to System.currentTimeMillis()))
            .await()
    }

    fun hiddenThoughtIdsFlow(uid: String): Flow<Set<String>> = callbackFlow {
        val reg = db.collection("users").document(uid).collection("hidden")
            .addSnapshotListener { snapshot, error ->
                trySend(if (error != null) emptySet() else snapshot?.documents?.map { it.id }?.toSet().orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun blockedUidsFlow(uid: String): Flow<Set<String>> = callbackFlow {
        val reg = db.collection("users").document(uid).collection("blocked")
            .addSnapshotListener { snapshot, error ->
                trySend(if (error != null) emptySet() else snapshot?.documents?.map { it.id }?.toSet().orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun getUserProfile(uid: String): User? {
        if (uid.isBlank()) return null
        val snap = db.collection("users").document(uid).get().await()
        return snap.toObject(User::class.java)?.copy(uid = snap.id)
    }

    fun authorThoughtsFlow(uid: String): Flow<List<Thought>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("thoughts")
            .whereEqualTo("authorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val t = doc.toObject(Thought::class.java) ?: return@mapNotNull null
                    if (t.id.isNotBlank()) t else t.copy(id = doc.id)
                }.orEmpty().sortedByDescending { it.createdAt }
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun notificationsFlow(uid: String): Flow<List<AppNotification>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("users").document(uid).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val n = doc.toObject(AppNotification::class.java) ?: return@mapNotNull null
                    if (n.id.isNotBlank()) n else n.copy(id = doc.id)
                }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun markNotificationRead(uid: String, notificationId: String) {
        if (uid.isBlank() || notificationId.isBlank()) return
        db.collection("users").document(uid).collection("notifications")
            .document(notificationId)
            .update("read", true)
            .await()
    }

    suspend fun submitSponsorLead(
        uid: String,
        businessName: String,
        email: String,
        city: String,
        budget: String,
        message: String,
    ) {
        db.collection("sponsor_leads").add(
            mapOf(
                "uid" to uid,
                "businessName" to businessName.trim(),
                "email" to email.trim(),
                "city" to city,
                "budget" to budget.trim(),
                "message" to message.trim(),
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun computeVoiceScore(user: User, myThoughts: List<Thought>): Int {
        val feels = myThoughts.sumOf { it.feelCount }
        return myThoughts.size * 10 + user.totalCommentsWritten * 5 + feels
    }

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun currentWeekId(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }
}
