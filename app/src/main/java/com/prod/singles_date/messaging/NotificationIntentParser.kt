package com.prod.singles_date.messaging

import android.content.Intent
import com.prod.singles_date.util.AppLinks

object NotificationIntentParser {
    fun parse(intent: Intent?): PendingNotification? {
        if (intent == null) return null
        val type = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        val promptText = intent.getStringExtra(EXTRA_PROMPT).orEmpty()
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty()
        val senderId = intent.getStringExtra(EXTRA_SENDER_ID).orEmpty()
        val thoughtId = intent.getStringExtra(EXTRA_THOUGHT_ID)
            ?: AppLinks.parse(intent.data)?.let { link ->
                when (link) {
                    is AppLinks.DeepLink.Thought -> link.thoughtId
                    else -> ""
                }
            }.orEmpty()

        if (type.isBlank() && thoughtId.isBlank() && promptText.isBlank() && conversationId.isBlank()) return null
        return PendingNotification(
            type = type,
            thoughtId = thoughtId,
            promptText = promptText,
            conversationId = conversationId,
            senderId = senderId,
        )
    }

    const val EXTRA_TYPE = "hoght_fcm_type"
    const val EXTRA_PROMPT = "hoght_fcm_prompt"
    const val EXTRA_THOUGHT_ID = "hoght_thought_id"
    const val EXTRA_CONVERSATION_ID = "hoght_conversation_id"
    const val EXTRA_SENDER_ID = "hoght_sender_id"
}
