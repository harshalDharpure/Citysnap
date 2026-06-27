package com.prod.singles_date

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.prod.singles_date.data.LocalPreferences
import com.prod.singles_date.messaging.HoghtMessagingService
import com.prod.singles_date.messaging.NotificationIntentParser
import com.prod.singles_date.messaging.PendingNotification
import com.prod.singles_date.model.ThemeMode
import androidx.compose.material3.MaterialTheme
import com.prod.singles_date.navigation.AppNavGraph
import com.prod.singles_date.ui.theme.CitysnapTheme
import com.prod.singles_date.util.AppLinks

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private var pendingDeepLink by mutableStateOf<AppLinks.DeepLink?>(null)
    private var pendingNotification by mutableStateOf<PendingNotification?>(null)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HoghtMessagingService.ensureChannel(this)
        requestNotificationPermissionIfNeeded()
        consumeIntent(intent)
        themeMode = LocalPreferences(this).getThemeMode()
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CitysnapTheme(darkTheme = darkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AppNavGraph(
                        pendingDeepLink = pendingDeepLink,
                        onDeepLinkHandled = { pendingDeepLink = null },
                        pendingNotification = pendingNotification,
                        onNotificationHandled = { pendingNotification = null },
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            LocalPreferences(this@MainActivity).setThemeMode(mode)
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        pendingDeepLink = AppLinks.parse(intent?.data)
        pendingNotification = NotificationIntentParser.parse(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
