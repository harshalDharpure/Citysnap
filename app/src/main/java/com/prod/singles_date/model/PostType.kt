package com.prod.singles_date.model

object PostType {
    const val SNAP = "snap"
    const val NOTE = "note"

    const val SNAP_MAX_LENGTH = 250
    const val NOTE_MAX_LENGTH = 800

    fun maxLength(type: String): Int = when (type) {
        NOTE -> NOTE_MAX_LENGTH
        else -> SNAP_MAX_LENGTH
    }

    fun isValid(type: String): Boolean = type == SNAP || type == NOTE
}
