package com.prod.singles_date.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prod.singles_date.model.ChatMessage
import com.prod.singles_date.model.ConversationMeta
import com.prod.singles_date.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    companion object {
        private const val TAG = "ChatRepository"
        const val MAX_MESSAGE_LENGTH = 1000

        fun conversationId(uidA: String, uidB: String): String {
            val sorted = listOf(uidA, uidB).sorted()
            return "${sorted[0]}_${sorted[1]}"
        }
    }

    suspend fun isBlocked(uidA: String, uidB: String): Boolean {
        if (uidA.isBlank() || uidB.isBlank() || uidA == uidB) return false
        val aBlocksB = db.collection("users").document(uidA).collection("blocked").document(uidB).get().await()
        if (aBlocksB.exists()) return true
        val bBlocksA = db.collection("users").document(uidB).collection("blocked").document(uidA).get().await()
        return bBlocksA.exists()
    }

    suspend fun getOrCreateConversation(myUid: String, otherUid: String): String {
        require(myUid.isNotBlank() && otherUid.isNotBlank()) { "User ids required" }
        require(myUid != otherUid) { "Cannot message yourself" }
        if (isBlocked(myUid, otherUid)) error("You can't message this user")

        val id = conversationId(myUid, otherUid)
        val ref = db.collection("conversations").document(id)
        val snap = ref.get().await()
        if (!snap.exists()) {
            val participants = listOf(myUid, otherUid).sorted()
            ref.set(
                mapOf(
                    "participantIds" to participants,
                    "participants" to mapOf(participants[0] to true, participants[1] to true),
                    "lastMessageText" to "",
                    "lastMessageAt" to System.currentTimeMillis(),
                    "lastSenderId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        return id
    }

    suspend fun sendMessage(conversationId: String, senderId: String, text: String) {
        val body = text.trim().take(MAX_MESSAGE_LENGTH)
        require(body.isNotBlank()) { "Message cannot be empty" }
        require(conversationId.isNotBlank() && senderId.isNotBlank()) { "Invalid message target" }

        val convoRef = db.collection("conversations").document(conversationId)
        val convoSnap = convoRef.get().await()
        val participantIds = convoSnap.get("participantIds") as? List<*>
            ?: error("Conversation not found")
        val participants = participantIds.filterIsInstance<String>()
        require(participants.contains(senderId)) { "Not a participant" }
        val recipientId = participants.firstOrNull { it != senderId }
            ?: error("Invalid conversation")

        val preview = if (body.length > 120) "${body.take(117)}..." else body
        val now = System.currentTimeMillis()

        val doc = convoRef.collection("messages").document()
        db.runTransaction { tx ->
            tx.set(
                doc,
                mapOf(
                    "senderId" to senderId,
                    "text" to body,
                    "createdAt" to now,
                ),
            )
            tx.update(
                convoRef,
                mapOf(
                    "lastMessageText" to preview,
                    "lastMessageAt" to now,
                    "lastSenderId" to senderId,
                ),
            )

            val senderMetaRef = db.collection("users").document(senderId)
                .collection("conversation_meta").document(conversationId)
            tx.set(
                senderMetaRef,
                mapOf(
                    "otherUserId" to recipientId,
                    "lastMessageText" to preview,
                    "lastMessageAt" to now,
                    "unreadCount" to 0,
                    "hidden" to false,
                ),
            )

            val recipientMetaRef = db.collection("users").document(recipientId)
                .collection("conversation_meta").document(conversationId)
            val recipientMeta = tx.get(recipientMetaRef)
            val recipientUnread = if (recipientMeta.exists()) {
                recipientMeta.getLong("unreadCount")?.toInt() ?: 0
            } else {
                0
            }
            tx.set(
                recipientMetaRef,
                mapOf(
                    "otherUserId" to senderId,
                    "lastMessageText" to preview,
                    "lastMessageAt" to now,
                    "unreadCount" to recipientUnread + 1,
                    "hidden" to false,
                ),
            )
        }.await()
    }

    fun inboxFlow(uid: String): Flow<List<ConversationMeta>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = db.collection("users").document(uid).collection("conversation_meta")
            .whereEqualTo("hidden", false)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "inboxFlow($uid) error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    ConversationMeta(
                        conversationId = doc.id,
                        otherUserId = doc.getString("otherUserId").orEmpty(),
                        lastMessageText = doc.getString("lastMessageText").orEmpty(),
                        lastMessageAt = doc.getLong("lastMessageAt") ?: 0L,
                        unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0,
                        hidden = doc.getBoolean("hidden") == true,
                    ).takeIf { it.otherUserId.isNotBlank() }
                }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun messagesFlow(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = db.collection("conversations").document(conversationId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "messagesFlow($conversationId) error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    ChatMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L,
                    ).takeIf { it.text.isNotBlank() }
                }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    suspend fun markConversationRead(uid: String, conversationId: String) {
        if (uid.isBlank() || conversationId.isBlank()) return
        runCatching {
            db.collection("users").document(uid).collection("conversation_meta").document(conversationId)
                .update("unreadCount", 0)
                .await()
        }.onFailure { Log.w(TAG, "markConversationRead failed", it) }
    }

    suspend fun hideConversation(uid: String, conversationId: String) {
        if (uid.isBlank() || conversationId.isBlank()) return
        runCatching {
            db.collection("users").document(uid).collection("conversation_meta").document(conversationId)
                .update("hidden", true)
                .await()
        }.onFailure { Log.w(TAG, "hideConversation failed", it) }
    }

    suspend fun reportMessage(
        reporterUid: String,
        conversationId: String,
        messageId: String,
        reportedUid: String,
        reason: String,
    ) {
        if (reporterUid.isBlank() || conversationId.isBlank() || messageId.isBlank()) return
        db.collection("reports").add(
            mapOf(
                "type" to "message",
                "reporterUid" to reporterUid,
                "conversationId" to conversationId,
                "messageId" to messageId,
                "reportedUid" to reportedUid,
                "reason" to reason.trim(),
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun searchUsersInCity(city: String, query: String, excludeUid: String): List<User> {
        if (city.isBlank() || query.isBlank()) return emptyList()
        val needle = query.trim().lowercase()
        val snap = db.collection("public_profiles")
            .whereEqualTo("city", city)
            .limit(40)
            .get()
            .await()
        return snap.documents.mapNotNull { doc ->
            val name = doc.getString("name").orEmpty()
            if (doc.id == excludeUid) return@mapNotNull null
            if (!name.lowercase().contains(needle)) return@mapNotNull null
            User(
                uid = doc.id,
                name = name,
                city = doc.getString("city").orEmpty(),
                locality = doc.getString("locality").orEmpty(),
                photoUrl = doc.getString("photoUrl").orEmpty(),
            )
        }.take(15)
    }

    suspend fun getPublicProfile(uid: String): User? {
        if (uid.isBlank()) return null
        val snap = db.collection("public_profiles").document(uid).get().await()
        if (!snap.exists()) return null
        return User(
            uid = snap.id,
            name = snap.getString("name").orEmpty(),
            city = snap.getString("city").orEmpty(),
            locality = snap.getString("locality").orEmpty(),
            photoUrl = snap.getString("photoUrl").orEmpty(),
        )
    }
}
