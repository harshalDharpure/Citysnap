package com.prod.singles_date.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

object AnalyticsEvents {
    fun logSignUp(analytics: FirebaseAnalytics, method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun logLogin(analytics: FirebaseAnalytics, method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun logPostCreated(analytics: FirebaseAnalytics, city: String, hasImages: Boolean) {
        analytics.logEvent("post_created") {
            param("city", city)
            param("has_images", if (hasImages) 1L else 0L)
        }
    }

    fun logScreen(analytics: FirebaseAnalytics, screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }

    private inline fun FirebaseAnalytics.logEvent(name: String, block: Bundle.() -> Unit) {
        logEvent(name, Bundle().apply(block))
    }

    private fun Bundle.param(key: String, value: String) {
        putString(key, value)
    }

    private fun Bundle.param(key: String, value: Long) {
        putLong(key, value)
    }
}
