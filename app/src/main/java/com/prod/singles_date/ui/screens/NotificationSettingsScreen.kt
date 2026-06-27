package com.prod.singles_date.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prod.singles_date.data.LocalPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    uid: String? = null,
    serverNotifyFeels: Boolean? = null,
    serverNotifyComments: Boolean? = null,
    serverNotifyPrompts: Boolean? = null,
    serverNotifyMessages: Boolean? = null,
    onUpdatePrefs: (
        notifyFeels: Boolean,
        notifyComments: Boolean,
        notifyPrompts: Boolean,
        notifyMessages: Boolean,
    ) -> Unit = { _, _, _, _ -> },
) {
    val context = LocalContext.current
    val prefs = remember { LocalPreferences(context) }
    val initialFeels = serverNotifyFeels ?: prefs.getNotifyFeels()
    val initialComments = serverNotifyComments ?: prefs.getNotifyComments()
    val initialPrompts = serverNotifyPrompts ?: prefs.getNotifyPrompts()
    val initialMessages = serverNotifyMessages ?: true
    var notifyFeels by remember(uid, serverNotifyFeels) { mutableStateOf(initialFeels) }
    var notifyComments by remember(uid, serverNotifyComments) { mutableStateOf(initialComments) }
    var notifyPrompts by remember(uid, serverNotifyPrompts) { mutableStateOf(initialPrompts) }
    var notifyMessages by remember(uid, serverNotifyMessages) { mutableStateOf(initialMessages) }

    fun persist(feels: Boolean, comments: Boolean, prompts: Boolean, messages: Boolean) {
        prefs.setNotifyFeels(feels)
        prefs.setNotifyComments(comments)
        prefs.setNotifyPrompts(prompts)
        if (!uid.isNullOrBlank()) {
            onUpdatePrefs(feels, comments, prompts, messages)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Notifications", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (uid != null) {
                    "Choose which push notifications you receive. Saved to your account."
                } else {
                    "Sign in to sync notification preferences across devices."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            NotificationToggle(
                title = "Feels on your posts",
                subtitle = "When someone feels your post or it hits a milestone",
                checked = notifyFeels,
                onCheckedChange = {
                    notifyFeels = it
                    persist(it, notifyComments, notifyPrompts, notifyMessages)
                },
            )
            NotificationToggle(
                title = "Comments",
                subtitle = "When someone comments on your post",
                checked = notifyComments,
                onCheckedChange = {
                    notifyComments = it
                    persist(notifyFeels, it, notifyPrompts, notifyMessages)
                },
            )
            NotificationToggle(
                title = "Direct messages",
                subtitle = "When someone sends you a private message",
                checked = notifyMessages,
                onCheckedChange = {
                    notifyMessages = it
                    persist(notifyFeels, notifyComments, notifyPrompts, it)
                },
            )
            NotificationToggle(
                title = "Daily prompts",
                subtitle = "Morning writing prompts and locality topics for your city",
                checked = notifyPrompts,
                onCheckedChange = {
                    notifyPrompts = it
                    persist(notifyFeels, notifyComments, it, notifyMessages)
                },
            )
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
