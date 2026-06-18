package com.prod.singles_date.util

import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.Thought

object FeedRanking {
    fun hotScore(thought: Thought, now: Long = System.currentTimeMillis()): Double {
        val ageHours = (now - thought.createdAt).coerceAtLeast(0L) / 3_600_000.0
        val recencyBonus = maxOf(0.0, 24.0 - ageHours) * 2.0
        return thought.feelCount * 3.0 +
            thought.commentCount * 5.0 +
            thought.shareCount * 8.0 +
            recencyBonus +
            if (thought.imageUrls.isNotEmpty()) 5.0 else 0.0
    }

    /**
     * Interleave locality-matched thoughts (~60%) with city-wide thoughts (~40%).
     */
    fun mixLocalAndCity(
        thoughts: List<Thought>,
        userLocality: String,
        mode: FeedSortMode,
        now: Long = System.currentTimeMillis(),
    ): List<Thought> {
        if (userLocality.isBlank()) {
            return sortThoughts(thoughts, mode, now)
        }
        val local = thoughts.filter { it.locality == userLocality }
        val cityWide = thoughts.filter { it.locality != userLocality }
        val sortedLocal = sortThoughts(local, mode, now)
        val sortedCity = sortThoughts(cityWide, mode, now)
        if (sortedLocal.isEmpty()) return sortedCity
        if (sortedCity.isEmpty()) return sortedLocal

        val result = mutableListOf<Thought>()
        var li = 0
        var ci = 0
        while (li < sortedLocal.size || ci < sortedCity.size) {
            repeat(3) {
                if (li < sortedLocal.size) result += sortedLocal[li++]
            }
            if (ci < sortedCity.size) result += sortedCity[ci++]
        }
        return result.distinctBy { it.id }
    }

    fun sortThoughts(
        thoughts: List<Thought>,
        mode: FeedSortMode,
        now: Long = System.currentTimeMillis(),
    ): List<Thought> = when (mode) {
        FeedSortMode.NEW -> thoughts.sortedByDescending { it.createdAt }
        FeedSortMode.HOT -> thoughts.sortedByDescending { hotScore(it, now) }
    }

    fun trendingLabel(thought: Thought, rank: Int, userLocality: String): String? {
        if (rank in 1..3) {
            return "🔥 #$rank in ${AppCity.displayName(thought.city)}"
        }
        if (hotScore(thought) > 80) return "🔥 Rising Fast"
        if (userLocality.isNotBlank() && thought.locality == userLocality && thought.feelCount >= 20) {
            return "🔥 Hot in ${AppLocality.displayName(thought.locality)}"
        }
        return null
    }
}

enum class FeedSortMode { NEW, HOT }
