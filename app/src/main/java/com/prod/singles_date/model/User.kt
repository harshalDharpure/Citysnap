package com.prod.singles_date.model

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val locality: String = "",
    val referralCode: String = "",
    val referredBy: String = "",
    val referralCount: Int = 0,
    val fcmToken: String = "",
    val postStreak: Int = 0,
    val lastPostDate: String = "",
    val voiceScore: Int = 0,
    val badges: List<String> = emptyList(),
    val totalFeelsReceived: Int = 0,
    val totalCommentsWritten: Int = 0,
    val isPremium: Boolean = false,
    val notifyFeels: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyPrompts: Boolean = true,
    val photoUrl: String = "",
    val savedThoughtIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
