package com.prod.singles_date.model

/** User badge ids stored in Firestore `users/{uid}.badges`. */
object Badge {
    const val EARLY_BANGALORE_VOICE = "early_bangalore_voice"
    const val VOICE_OF_BANGALORE = "voice_of_bangalore"
    const val OFFICE_INSIDER = "office_insider"
    const val TRAFFIC_PHILOSOPHER = "traffic_philosopher"
    const val RENT_SURVIVOR = "rent_survivor"
    const val TOP_CONTRIBUTOR = "top_contributor"

    fun displayName(badgeId: String): String = when (badgeId) {
        EARLY_BANGALORE_VOICE -> "Early Bangalore Voice"
        VOICE_OF_BANGALORE -> "Voice of Bangalore"
        OFFICE_INSIDER -> "Office Insider"
        TRAFFIC_PHILOSOPHER -> "Traffic Philosopher"
        RENT_SURVIVOR -> "Rent Survivor"
        TOP_CONTRIBUTOR -> "Top Contributor"
        else -> badgeId.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    fun emoji(badgeId: String): String = when (badgeId) {
        EARLY_BANGALORE_VOICE -> "🎤"
        VOICE_OF_BANGALORE -> "🔥"
        OFFICE_INSIDER -> "🏢"
        TRAFFIC_PHILOSOPHER -> "🚦"
        RENT_SURVIVOR -> "🏠"
        TOP_CONTRIBUTOR -> "⭐"
        else -> "🏅"
    }
}
