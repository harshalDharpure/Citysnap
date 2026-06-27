package com.prod.singles_date.model

import androidx.annotation.Keep

@Keep
data class AppNotification(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val thoughtId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
