package com.prod.singles_date.model

/** Topic tags for thoughts. Stored as lowercase ids in Firestore. */
object ThoughtCategory {
    const val WORK = "work"
    const val LIFE = "life"
    const val RELATIONSHIP = "relationship"
    const val COLLEGE = "college"
    const val TRAFFIC = "traffic"
    const val RENT = "rent"
    const val STARTUP = "startup"
    const val FOOD = "food"

    val ALL = listOf(
        WORK,
        LIFE,
        RELATIONSHIP,
        COLLEGE,
        TRAFFIC,
        RENT,
        STARTUP,
        FOOD,
    )

    fun displayName(categoryId: String): String = when (categoryId) {
        WORK -> "Work"
        LIFE -> "Life"
        RELATIONSHIP -> "Relationship"
        COLLEGE -> "College"
        TRAFFIC -> "Traffic"
        RENT -> "Rent"
        STARTUP -> "Startup"
        FOOD -> "Food"
        else -> categoryId.replaceFirstChar { it.uppercase() }
    }

    fun emoji(categoryId: String): String = when (categoryId) {
        WORK -> "🏢"
        LIFE -> "💬"
        RELATIONSHIP -> "❤️"
        COLLEGE -> "🎓"
        TRAFFIC -> "🚦"
        RENT -> "🏠"
        STARTUP -> "🚀"
        FOOD -> "🍜"
        else -> ""
    }

    fun isValid(categoryId: String): Boolean = categoryId.isBlank() || categoryId in ALL
}
