package com.prod.singles_date.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.prod.singles_date.model.ChatMessage
import com.prod.singles_date.ui.components.MessageBubble
import com.prod.singles_date.ui.components.ReportReasonDialog
import com.prod.singles_date.viewmodel.ChatViewModel
import com.prod.singles_date.viewmodel.ThoughtViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    conversationId: String,
    otherUid: String,
    currentUid: String?,
    chatViewModel: ChatViewModel,
    thoughtViewModel: ThoughtViewModel,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
) {
    if (currentUid.isNullOrBlank()) {
        LaunchedEffect(Unit) { onRequireLogin() }
        return
    }

    LaunchedEffect(conversationId, otherUid) {
        chatViewModel.setActiveChat(conversationId, otherUid)
    }

    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val otherUser by chatViewModel.otherUser.collectAsStateWithLifecycle()
    val isSending by chatViewModel.isSending.collectAsStateWithLifecycle()
    val error by chatViewModel.error.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val headerName = otherUser?.name?.ifBlank { "Chat" } ?: "Chat"
    val headerCity = otherUser?.city?.takeIf { it.isNotBlank() }?.let { AppCity.displayName(it) }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(headerName, fontWeight = FontWeight.SemiBold)
                        if (headerCity.isNotBlank()) {
                            Text(
                                headerCity,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Block user") },
                            onClick = {
                                menuOpen = false
                                thoughtViewModel.blockAuthor(currentUid, otherUid)
                                onBack()
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(1000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message…") },
                    maxLines = 4,
                    enabled = !isSending,
                )
                IconButton(
                    onClick = {
                        val text = draft
                        if (text.isNotBlank()) {
                            chatViewModel.sendMessage(text)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank() && !isSending,
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (messages.isEmpty()) {
                Text(
                    text = "Say hi to $headerName",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            text = message.text,
                            isMine = message.senderId == currentUid,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (message.senderId != currentUid) {
                                        reportTarget = message
                                    }
                                },
                            ),
                        )
                    }
                }
            }
            if (!error.isNullOrBlank()) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                )
            }
        }
    }

    reportTarget?.let { message ->
        ReportReasonDialog(
            title = "Report message",
            onDismiss = { reportTarget = null },
            onConfirm = { reason ->
                chatViewModel.reportMessage(message.id, reason)
                reportTarget = null
            },
        )
    }
}
