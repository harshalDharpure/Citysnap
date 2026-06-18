package com.prod.singles_date.model

import androidx.annotation.Keep

@Keep
data class Thought(
    val id: String = "",
    val text: String = "",
    val feelCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val city: String = "",
    val locality: String = "",
    val category: String = "",
    val trendingRank: Int = 0,
    val isSponsored: Boolean = false,
    val sponsorLabel: String = "",
    val imageUrls: List<String> = emptyList(),
    val imageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun displayAuthorName(): String = authorName.ifBlank { "User" }
}
