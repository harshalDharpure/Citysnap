package com.prod.singles_date.navigation



object Routes {

    const val Splash = "splash"

    const val Welcome = "welcome"

    const val CitySelect = "city_select"

    const val Login = "login"

    const val Signup = "signup"

    const val Feed = "feed"

    const val Profile = "profile"

    const val PostDetail = "post_detail/{thoughtId}"

    const val BlockedUsers = "blocked_users"

    const val NotificationSettings = "notification_settings"

    const val LegalGuidelines = "legal_guidelines"

    const val LegalPrivacy = "legal_privacy"



    fun postDetail(thoughtId: String) = "post_detail/$thoughtId"

}


