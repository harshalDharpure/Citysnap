package com.prod.singles_date.util

import com.prod.singles_date.model.Thought
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedRankingTest {
    private val now = 1_700_000_000_000L

    @Test
    fun sortThoughts_new_ordersByCreatedAtDescending() {
        val thoughts = listOf(
            thought(id = "a", createdAt = now - 1000, feelCount = 50),
            thought(id = "b", createdAt = now, feelCount = 0),
            thought(id = "c", createdAt = now - 500, feelCount = 10),
        )
        val sorted = FeedRanking.sortThoughts(thoughts, FeedSortMode.NEW, now)
        assertEquals(listOf("b", "c", "a"), sorted.map { it.id })
    }

    @Test
    fun sortThoughts_hot_prefersHighEngagement() {
        val thoughts = listOf(
            thought(id = "stale", createdAt = now - 72 * 3_600_000, feelCount = 20),
            thought(
                id = "rising",
                createdAt = now - 2_000,
                feelCount = 40,
                commentCount = 8,
                shareCount = 3,
            ),
        )
        val sorted = FeedRanking.sortThoughts(thoughts, FeedSortMode.HOT, now)
        assertEquals("rising", sorted.first().id)
    }

    @Test
    fun hotScore_boostsRecentPostsWithImages() {
        val withImage = thought(createdAt = now - 3_600_000, feelCount = 2, imageUrls = listOf("x"))
        val textOnly = thought(createdAt = now - 3_600_000, feelCount = 2)
        assertTrue(FeedRanking.hotScore(withImage, now) > FeedRanking.hotScore(textOnly, now))
    }

    @Test
    fun mixLocalAndCity_interleavesLocalThoughts() {
        val thoughts = listOf(
            thought(id = "l1", locality = "hsr", createdAt = now),
            thought(id = "l2", locality = "hsr", createdAt = now - 1),
            thought(id = "c1", locality = "koramangala", createdAt = now - 2),
        )
        val mixed = FeedRanking.mixLocalAndCity(thoughts, "hsr", FeedSortMode.NEW, now)
        assertTrue(mixed.first().locality == "hsr")
        assertEquals(3, mixed.size)
    }

    private fun thought(
        id: String = "t1",
        createdAt: Long = now,
        feelCount: Int = 0,
        commentCount: Int = 0,
        shareCount: Int = 0,
        locality: String = "hsr",
        imageUrls: List<String> = emptyList(),
    ) = Thought(
        id = id,
        text = "sample",
        authorId = "u1",
        authorName = "User",
        city = "bangalore",
        locality = locality,
        createdAt = createdAt,
        feelCount = feelCount,
        commentCount = commentCount,
        shareCount = shareCount,
        imageUrls = imageUrls,
    )
}
