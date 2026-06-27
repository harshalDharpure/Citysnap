package com.prod.singles_date.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prod.singles_date.model.User
import com.prod.singles_date.ui.components.ConversationRow
import com.prod.singles_date.ui.components.MainBottomBar
import com.prod.singles_date.ui.components.MainTab
import com.prod.singles_date.ui.components.NewMessageSheet
import com.prod.singles_date.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    chatViewModel: ChatViewModel,
    currentUser: User?,
    isLoggedIn: Boolean,
    activeCity: String,
    onOpenChat: (conversationId: String, otherUid: String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFeed: () -> Unit,
    onChangeCity: () -> Unit,
    onOpenSnap: () -> Unit,
    onRequireLogin: () -> Unit,
) {
    val inbox by chatViewModel.inbox.collectAsStateWithLifecycle()
    val profileCache by chatViewModel.profileCache.collectAsStateWithLifecycle()
    var showNewMessage by remember { mutableStateOf(false) }
    val searchResults by chatViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by chatViewModel.isSearching.collectAsStateWithLifecycle()

    if (!isLoggedIn) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Messages", fontWeight = FontWeight.SemiBold) })
            },
            bottomBar = {
                MainBottomBar(
                    selectedTab = MainTab.Messages,
                    profilePhotoUrl = currentUser?.photoUrl.orEmpty(),
                    profileName = currentUser?.name.orEmpty(),
                    onTabSelected = { tab ->
                        when (tab) {
                            MainTab.Home -> onOpenFeed()
                            MainTab.Messages -> Unit
                            MainTab.Snap -> onOpenSnap()
                            MainTab.City -> onChangeCity()
                            MainTab.Profile -> onOpenProfile()
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ColumnContent(
                    title = "Sign in to message",
                    subtitle = "Chat privately with people in your city.",
                    action = { TextButton(onClick = onRequireLogin) { Text("Log in") } },
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { showNewMessage = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "New message")
                    }
                },
            )
        },
        bottomBar = {
            MainBottomBar(
                selectedTab = MainTab.Messages,
                profilePhotoUrl = currentUser?.photoUrl.orEmpty(),
                profileName = currentUser?.name.orEmpty(),
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.Home -> onOpenFeed()
                        MainTab.Messages -> Unit
                        MainTab.Snap -> onOpenSnap()
                        MainTab.City -> onChangeCity()
                        MainTab.Profile -> onOpenProfile()
                    }
                },
            )
        },
    ) { padding ->
        if (inbox.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ColumnContent(
                    title = "No messages yet",
                    subtitle = "Tap someone's profile and message them, or start a new chat.",
                    action = {
                        TextButton(onClick = { showNewMessage = true }) {
                            Text("New message")
                        }
                    },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(inbox, key = { it.conversationId }) { meta ->
                    val other = profileCache[meta.otherUserId]
                    ConversationRow(
                        meta = meta,
                        otherUserName = other?.name.orEmpty(),
                        otherUserPhotoUrl = other?.photoUrl.orEmpty(),
                        onClick = { onOpenChat(meta.conversationId, meta.otherUserId) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (showNewMessage) {
        NewMessageSheet(
            city = activeCity,
            isSearching = isSearching,
            results = searchResults,
            onSearch = { chatViewModel.searchUsers(activeCity, it) },
            onSelectUser = { user ->
                showNewMessage = false
                chatViewModel.clearSearch()
                chatViewModel.openChatWith(user.uid) { conversationId ->
                    onOpenChat(conversationId, user.uid)
                }
            },
            onDismiss = {
                showNewMessage = false
                chatViewModel.clearSearch()
            },
        )
    }
}

@Composable
private fun ColumnContent(
    title: String,
    subtitle: String,
    action: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        action()
    }
}
