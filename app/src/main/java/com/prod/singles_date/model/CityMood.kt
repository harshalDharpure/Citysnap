package com.prod.singles_date.model

import androidx.annotation.Keep

/** Weekly city mood summary written by Cloud Functions. */
@Keep
data class CityMood(
    val city: String = "",
    val weekId: String = "",
    val lines: List<String> = emptyList(),
    val topCategory: String = "",
    val createdAt: Long = 0L,
)
