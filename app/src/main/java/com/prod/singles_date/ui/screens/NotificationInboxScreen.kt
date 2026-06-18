package com.prod.singles_date.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prod.singles_date.viewmodel.ThoughtViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    thoughtViewModel: ThoughtViewModel,
    currentUid: String,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
) {
    val notifications by thoughtViewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No notifications yet.\nPost something — feels and comments show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(notifications, key = { it.id }) { n ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                thoughtViewModel.markNotificationRead(currentUid, n.id)
                                if (n.thoughtId.isNotBlank()) onOpenPost(n.thoughtId)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            n.title.ifBlank { "Citysnap" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (n.read) FontWeight.Normal else FontWeight.SemiBold,
                        )
                        Text(
                            n.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            relativeTime(n.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun relativeTime(createdAt: Long): String {
    val diff = System.currentTimeMillis() - createdAt
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        hours < 1 -> "just now"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
