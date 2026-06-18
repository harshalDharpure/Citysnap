package com.prod.singles_date.util

import android.net.Uri

/** Deep-link and share URL helpers for Citysnap. */
object AppLinks {
    const val WEB_HOST = "citysnap.app"
    const val LEGACY_WEB_HOST = "hoght.app"
    const val WEB_BASE = "https://$WEB_HOST"

    fun thoughtUrl(thoughtId: String): String = "$WEB_BASE/t/$thoughtId"

    fun inviteUrl(referralCode: String): String = "$WEB_BASE/invite/${referralCode.lowercase()}"

    sealed interface DeepLink {
        data class Thought(val thoughtId: String) : DeepLink
        data class Invite(val referralCode: String) : DeepLink
    }

    fun parse(uri: Uri?): DeepLink? {
        if (uri == null) return null

        val segments = uri.pathSegments.filter { it.isNotBlank() }

        if ((uri.host == WEB_HOST || uri.host == LEGACY_WEB_HOST) && segments.size >= 2) {
            return when (segments[0]) {
                "t" -> DeepLink.Thought(segments[1])
                "invite" -> DeepLink.Invite(segments[1])
                else -> null
            }
        }

        if ((uri.scheme == "citysnap" || uri.scheme == "hoght") && uri.host == "app" && segments.size >= 2) {
            return when (segments[0]) {
                "t" -> DeepLink.Thought(segments[1])
                "invite" -> DeepLink.Invite(segments[1])
                else -> null
            }
        }

        return null
    }
}
