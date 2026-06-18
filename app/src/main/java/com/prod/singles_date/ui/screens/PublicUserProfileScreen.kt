package com.prod.singles_date.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.Badge
import com.prod.singles_date.model.User
import com.prod.singles_date.ui.components.ThoughtCard
import com.prod.singles_date.ui.components.UserAvatar
import com.prod.singles_date.viewmodel.ThoughtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUserProfileScreen(
    authorUid: String,
    currentUid: String?,
    thoughtViewModel: ThoughtViewModel,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onRequireLogin: () -> Unit,
) {
    val posts by thoughtViewModel.publicProfileThoughts.collectAsStateWithLifecycle()
    val feeledIds by thoughtViewModel.feeledThoughtIds.collectAsStateWithLifecycle()
    var profile by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(authorUid) {
        thoughtViewModel.setPublicProfileUid(authorUid)
        loading = true
        profile = thoughtViewModel.fetchUserProfile(authorUid)
        loading = false
    }

    val user = profile
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.name?.ifBlank { "Profile" } ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (loading && user == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UserAvatar(name = user?.name.orEmpty(), photoUrl = user?.photoUrl.orEmpty(), size = 72.dp)
                    Text(user?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (user?.city?.isNotBlank() == true) {
                        Text(
                            "${AppCity.displayName(user.city)}${user.locality.takeIf { it.isNotBlank() }?.let { " · ${AppLocality.displayName(it)}" } ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Voice score ${user?.voiceScore ?: 0} · ${posts.size} posts · ${user?.postStreak ?: 0}-day streak",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    user?.badges?.takeIf { it.isNotEmpty() }?.forEach { badgeId ->
                        Text(
                            "${Badge.emoji(badgeId)} ${Badge.displayName(badgeId)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            items(posts, key = { it.id }) { thought ->
                ThoughtCard(
                    thought = thought,
                    hasFeeled = thought.id in feeledIds,
                    userLocality = user?.locality.orEmpty(),
                    rank = 0,
                    authorPhotoUrl = user?.photoUrl.orEmpty(),
                    onFeelThis = {
                        if (currentUid != null) thoughtViewModel.onFeelThis(thought.id, currentUid)
                        else onRequireLogin()
                    },
                    onCopy = {},
                    onShareImage = {},
                    onWhatsApp = {},
                    onComment = { onOpenPost(thought.id) },
                    onEdit = null,
                    showComments = true,
                    showFeelButton = true,
                    onOpenDetail = { onOpenPost(thought.id) },
                )
            }
        }
    }
}
