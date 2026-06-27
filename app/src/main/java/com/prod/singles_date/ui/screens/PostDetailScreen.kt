package com.prod.singles_date.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prod.singles_date.model.ThoughtLoadState
import com.prod.singles_date.model.User
import com.prod.singles_date.ui.components.EditDialog
import com.prod.singles_date.ui.components.ReportReasonDialog
import com.prod.singles_date.ui.components.ThoughtCard
import com.prod.singles_date.ui.util.copyThoughtToClipboard
import com.prod.singles_date.ui.util.shareThoughtAsImage
import com.prod.singles_date.ui.util.shareThoughtToWhatsApp
import com.prod.singles_date.viewmodel.ThoughtViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    thoughtId: String,
    thoughtViewModel: ThoughtViewModel,
    currentUser: User?,
    currentUid: String?,
    isLoggedIn: Boolean,
    activeCity: String,
    activeLocality: String,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    onMessageAuthor: ((authorId: String) -> Unit)? = null,
) {
    LaunchedEffect(thoughtId) {
        thoughtViewModel.setActiveDetailThoughtId(thoughtId)
    }
    DisposableEffect(Unit) {
        onDispose { thoughtViewModel.clearActiveDetailThoughtId() }
    }

    val loadState by thoughtViewModel.detailLoadState.collectAsStateWithLifecycle()
    val comments by thoughtViewModel.detailComments.collectAsStateWithLifecycle()
    val feeledIds by thoughtViewModel.feeledThoughtIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var draft by remember { mutableStateOf("") }
    var reportPostId by remember { mutableStateOf<String?>(null) }
    var reportComment by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteCommentId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = reportPostId != null || reportComment != null ||
        editingCommentId != null || pendingDeleteCommentId != null) {
        when {
            reportPostId != null -> reportPostId = null
            reportComment != null -> reportComment = null
            editingCommentId != null -> editingCommentId = null
            pendingDeleteCommentId != null -> pendingDeleteCommentId = null
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Post", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = loadState) {
            is ThoughtLoadState.Ready -> {
                val t = state.thought
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        ThoughtCard(
                            thought = t,
                            hasFeeled = t.id in feeledIds,
                            userLocality = activeLocality,
                            rank = 0,
                            onFeelThis = {
                                if (isLoggedIn && currentUid != null) {
                                    thoughtViewModel.onFeelThis(t.id, currentUid)
                                } else onRequireLogin()
                            },
                            onCopy = {
                                copyThoughtToClipboard(context, t.text, t.id, t.city.ifBlank { activeCity })
                                thoughtViewModel.incrementShareCount(t.id)
                            },
                            onShareImage = {
                                scope.launch {
                                    shareThoughtAsImage(
                                        context = context,
                                        thoughtText = t.text,
                                        authorName = t.displayAuthorName(),
                                        cityId = t.city.ifBlank { activeCity },
                                        feelCount = t.feelCount,
                                        imageUrl = t.imageUrls.firstOrNull(),
                                    )
                                    thoughtViewModel.incrementShareCount(t.id)
                                }
                            },
                            onWhatsApp = {
                                shareThoughtToWhatsApp(context, t.text, t.id, t.city.ifBlank { activeCity })
                                thoughtViewModel.incrementShareCount(t.id)
                            },
                            onComment = { },
                            showComments = true,
                            showFeelButton = true,
                            onOpenDetail = null,
                            onEdit = null,
                            onDelete = null,
                            onReport = if (isLoggedIn && currentUid != null && t.authorId != currentUid) {
                                { reportPostId = t.id }
                            } else null,
                            onBlock = if (isLoggedIn && currentUid != null && t.authorId != currentUid) {
                                { thoughtViewModel.blockAuthor(currentUid, t.authorId) }
                            } else null,
                        )
                    }

                    if (onMessageAuthor != null && isLoggedIn && currentUid != null && t.authorId != currentUid) {
                        item {
                            TextButton(
                                onClick = { onMessageAuthor(t.authorId) },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            ) {
                                Text("Message author")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Comments (${comments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "No comments yet. Be the first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        items(comments, key = { it.id }) { comment ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = comment.userName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    if (isLoggedIn && currentUid != null) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (comment.userId == currentUid) {
                                                TextButton(onClick = { editingCommentId = comment.id }) {
                                                    Text("Edit")
                                                }
                                                TextButton(onClick = { pendingDeleteCommentId = comment.id }) {
                                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                                }
                                            } else {
                                                TextButton(
                                                    onClick = {
                                                        reportComment = comment.id to comment.userId
                                                    },
                                                ) {
                                                    Text("Report", color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }

                    if (isLoggedIn && currentUid != null && currentUser != null) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { draft = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "Write a comment…",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    minLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val body = draft.trim()
                                        if (body.isNotEmpty()) {
                                            thoughtViewModel.addComment(t.id, currentUid, currentUser.name, body)
                                            draft = ""
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text("Post comment")
                                }
                            }
                        }
                    } else {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Sign in to join the conversation",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = onRequireLogin),
                                )
                            }
                        }
                    }
                }
            }
            ThoughtLoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            ThoughtLoadState.NotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "This post isn't available.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "It may have been deleted or the link is invalid.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Go back")
                    }
                }
            }
        }
    }

    val postId = reportPostId
    if (postId != null && currentUid != null) {
        ReportReasonDialog(
            title = "Report post",
            onDismiss = { reportPostId = null },
            onConfirm = { reason ->
                thoughtViewModel.reportThought(postId, currentUid, reason)
                reportPostId = null
            },
        )
    }

    val commentReport = reportComment
    if (commentReport != null && currentUid != null) {
        val (commentId, _) = commentReport
        ReportReasonDialog(
            title = "Report comment",
            onDismiss = { reportComment = null },
            onConfirm = { reason ->
                thoughtViewModel.reportComment(thoughtId, commentId, currentUid, reason)
                reportComment = null
            },
        )
    }

    val editCommentId = editingCommentId
    val editComment = editCommentId?.let { id -> comments.find { it.id == id } }
    if (editComment != null && currentUid != null && editComment.userId == currentUid) {
        EditDialog(
            initialText = editComment.text,
            title = "Edit comment",
            subtitle = "Only you can update your comment",
            maxLength = ThoughtViewModel.MAX_COMMENT_LENGTH,
            onDismiss = { editingCommentId = null },
            onSave = { new ->
                thoughtViewModel.updateComment(thoughtId, editComment.id, new)
                editingCommentId = null
            },
        )
    }

    val deleteCommentId = pendingDeleteCommentId
    val deleteComment = deleteCommentId?.let { id -> comments.find { it.id == id } }
    if (
        deleteComment != null &&
        currentUid != null &&
        deleteComment.userId == currentUid
    ) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCommentId = null },
            title = { Text("Delete comment?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        thoughtViewModel.deleteComment(thoughtId, deleteComment.id)
                        pendingDeleteCommentId = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCommentId = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
