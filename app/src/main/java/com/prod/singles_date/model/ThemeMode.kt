package com.prod.singles_date.model

enum class ThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.find { it.storageKey == value } ?: SYSTEM
    }
}
