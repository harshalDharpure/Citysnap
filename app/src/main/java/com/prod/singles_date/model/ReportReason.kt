package com.prod.singles_date.model

object ReportReason {
    const val INAPPROPRIATE = "Inappropriate content"
    const val SPAM = "Spam"
    const val HARASSMENT = "Harassment"
    const val HATE = "Hate speech"
    const val MISINFORMATION = "Misinformation"
    const val OTHER = "Other"

    val ALL = listOf(INAPPROPRIATE, SPAM, HARASSMENT, HATE, MISINFORMATION, OTHER)

    fun isValid(reason: String): Boolean = reason in ALL
}
