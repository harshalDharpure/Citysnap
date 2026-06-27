package com.prod.singles_date.model

import androidx.annotation.Keep

@Keep
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
