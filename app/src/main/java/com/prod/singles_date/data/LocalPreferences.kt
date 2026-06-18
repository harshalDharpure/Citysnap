package com.prod.singles_date.data

import android.content.Context
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.ThemeMode

/**
 * Guest city/locality and pending referral codes from invite deep links.
 * Logged-in users persist these on their Firestore profile instead.
 */
class LocalPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getGuestCity(): String = prefs.getString(KEY_GUEST_CITY, "").orEmpty()

    fun setGuestCity(cityId: String) {
        prefs.edit().putString(KEY_GUEST_CITY, cityId).apply()
    }

    fun getGuestLocality(): String = prefs.getString(KEY_GUEST_LOCALITY, "").orEmpty()

    fun setGuestLocality(localityId: String) {
        prefs.edit().putString(KEY_GUEST_LOCALITY, localityId).apply()
    }

    /** Writes guest onboarding in one commit so navigation can read it immediately. */
    fun saveGuestOnboarding(cityId: String, localityId: String) {
        prefs.edit()
            .putString(KEY_GUEST_CITY, cityId)
            .putString(KEY_GUEST_LOCALITY, localityId)
            .remove(KEY_GUEST_IDENTITY)
            .commit()
    }

    fun hasCompletedOnboarding(): Boolean {
        val city = getGuestCity()
        if (city.isBlank()) return false
        val requiresLocality = AppLocality.localitiesForCity(city).isNotEmpty()
        return !requiresLocality || getGuestLocality().isNotBlank()
    }

    fun getPendingReferralCode(): String = prefs.getString(KEY_PENDING_REFERRAL, "").orEmpty()

    fun setPendingReferralCode(code: String) {
        prefs.edit().putString(KEY_PENDING_REFERRAL, code.trim().lowercase()).apply()
    }

    fun clearPendingReferralCode() {
        prefs.edit().remove(KEY_PENDING_REFERRAL).apply()
    }

    fun getLastPromptDay(): Int = prefs.getInt(KEY_LAST_PROMPT_DAY, 0)

    fun setLastPromptDay(dayOfYear: Int) {
        prefs.edit().putInt(KEY_LAST_PROMPT_DAY, dayOfYear).apply()
    }

    fun getFeedLocalityFilter(): String = prefs.getString(KEY_FEED_LOCALITY, "").orEmpty()

    fun setFeedLocalityFilter(localityId: String) {
        prefs.edit().putString(KEY_FEED_LOCALITY, localityId).apply()
    }

    fun getFeedCategoryFilter(): String = prefs.getString(KEY_FEED_CATEGORY, "").orEmpty()

    fun setFeedCategoryFilter(categoryId: String) {
        prefs.edit().putString(KEY_FEED_CATEGORY, categoryId).apply()
    }

    fun getThemeMode(): ThemeMode = ThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, null))

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
    }

    fun getNotifyFeels(): Boolean = prefs.getBoolean(KEY_NOTIFY_FEELS, true)

    fun setNotifyFeels(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_FEELS, enabled).apply()
    }

    fun getNotifyComments(): Boolean = prefs.getBoolean(KEY_NOTIFY_COMMENTS, true)

    fun setNotifyComments(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_COMMENTS, enabled).apply()
    }

    fun getNotifyPrompts(): Boolean = prefs.getBoolean(KEY_NOTIFY_PROMPTS, true)

    fun setNotifyPrompts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_PROMPTS, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "hoght_prefs"
        private const val KEY_GUEST_CITY = "guest_city"
        private const val KEY_GUEST_LOCALITY = "guest_locality"
        private const val KEY_GUEST_IDENTITY = "guest_identity"
        private const val KEY_PENDING_REFERRAL = "pending_referral"
        private const val KEY_LAST_PROMPT_DAY = "last_prompt_day"
        private const val KEY_FEED_LOCALITY = "feed_locality_filter"
        private const val KEY_FEED_CATEGORY = "feed_category_filter"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NOTIFY_FEELS = "notify_feels"
        private const val KEY_NOTIFY_COMMENTS = "notify_comments"
        private const val KEY_NOTIFY_PROMPTS = "notify_prompts"

        fun referralCodeForUid(uid: String): String = uid.take(8).lowercase()

        fun defaultCityIfBlank(city: String): String =
            city.ifBlank { AppCity.BANGALORE }
    }
}
