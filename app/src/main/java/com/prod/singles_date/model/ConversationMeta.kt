package com.prod.singles_date.model

import androidx.annotation.Keep

@Keep
data class ConversationMeta(
    val conversationId: String = "",
    val otherUserId: String = "",
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0,
    val hidden: Boolean = false,
)
