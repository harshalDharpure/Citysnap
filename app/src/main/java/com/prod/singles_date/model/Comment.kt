package com.prod.singles_date.model

import androidx.annotation.Keep

@Keep
data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
