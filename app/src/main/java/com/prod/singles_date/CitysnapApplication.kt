package com.prod.singles_date

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CitysnapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        installAppCheck()
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
    }

    private fun installAppCheck() {
        AppCheckInstaller.install(FirebaseAppCheck.getInstance())
    }
}
