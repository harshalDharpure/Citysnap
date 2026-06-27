package com.prod.singles_date.model

/** Supported cities. Stored as lowercase ids in Firestore. Marketing focuses on Bangalore first. */
object AppCity {
    const val BANGALORE = "bangalore"
    const val PUNE = "pune"
    const val HYDERABAD = "hyderabad"
    const val CHENNAI = "chennai"
    const val MUMBAI = "mumbai"
    const val DELHI = "delhi"

    val ALL = listOf(BANGALORE, PUNE, HYDERABAD, CHENNAI, MUMBAI, DELHI)

    /** Cities shown prominently during Bangalore-first launch. */
    val MARKETING_FOCUS = BANGALORE

    fun displayName(cityId: String): String = when (cityId) {
        BANGALORE -> "Bangalore"
        PUNE -> "Pune"
        HYDERABAD -> "Hyderabad"
        CHENNAI -> "Chennai"
        MUMBAI -> "Mumbai"
        DELHI -> "Delhi"
        else -> cityId.replaceFirstChar { it.uppercase() }
    }

    fun isValid(cityId: String): Boolean = cityId in ALL

    fun isExpansionUnlocked(cityId: String): Boolean = cityId in ALL
}
