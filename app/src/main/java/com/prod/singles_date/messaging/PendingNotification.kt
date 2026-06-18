package com.prod.singles_date.messaging

/** Parsed FCM / notification tap payload passed into the UI layer. */
data class PendingNotification(
    val type: String = "",
    val thoughtId: String = "",
    val promptText: String = "",
) {
    val isDailyPrompt: Boolean get() = type == "daily_prompt" && promptText.isNotBlank()
    val isThoughtDeepLink: Boolean get() = thoughtId.isNotBlank()
}
