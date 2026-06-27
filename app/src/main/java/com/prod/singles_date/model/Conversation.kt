package com.prod.singles_date.model

import androidx.annotation.Keep

@Keep
data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
    val lastSenderId: String = "",
)
