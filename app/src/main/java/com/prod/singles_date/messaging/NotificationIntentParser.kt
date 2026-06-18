package com.prod.singles_date.messaging

import android.content.Intent
import com.prod.singles_date.util.AppLinks

object NotificationIntentParser {
    fun parse(intent: Intent?): PendingNotification? {
        if (intent == null) return null
        val type = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        val promptText = intent.getStringExtra(EXTRA_PROMPT).orEmpty()
        val thoughtId = intent.getStringExtra(EXTRA_THOUGHT_ID)
            ?: AppLinks.parse(intent.data)?.let { link ->
                when (link) {
                    is AppLinks.DeepLink.Thought -> link.thoughtId
                    else -> ""
                }
            }.orEmpty()

        if (type.isBlank() && thoughtId.isBlank() && promptText.isBlank()) return null
        return PendingNotification(type = type, thoughtId = thoughtId, promptText = promptText)
    }

    const val EXTRA_TYPE = "hoght_fcm_type"
    const val EXTRA_PROMPT = "hoght_fcm_prompt"
    const val EXTRA_THOUGHT_ID = "hoght_thought_id"
}
