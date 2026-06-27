package com.prod.singles_date.ui.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor

/** Compact counts for action icons — e.g. 2.9K, 3K (Instagram style) */
fun formatIgCountCompact(count: Int): String {
    if (count < 1_000) return count.toString()
    if (count < 10_000) {
        val thousands = count / 1_000f
        val rounded = floor(thousands * 10f) / 10f
        return if (rounded == rounded.toLong().toFloat()) {
            "${rounded.toLong()}K"
        } else {
            String.format(Locale.US, "%.1fK", rounded)
        }
    }
    if (count < 1_000_000) return "${count / 1_000}K"
    val millions = count / 1_000_000f
    return String.format(Locale.US, "%.1fM", millions)
}

/** Full counts for summary lines — e.g. 2,947 */
fun formatIgCountFull(count: Int): String =
    NumberFormat.getNumberInstance(Locale.US).format(count)
