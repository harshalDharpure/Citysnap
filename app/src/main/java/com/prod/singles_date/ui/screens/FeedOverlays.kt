package com.prod.singles_date.ui.screens

import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.Thought
import com.prod.singles_date.model.User
import com.prod.singles_date.ui.components.EditDialog
import com.prod.singles_date.ui.components.PostDialog
import com.prod.singles_date.ui.components.ReportReasonDialog

@Composable
fun FeedOverlays(
    showPost: Boolean,
    isLoggedIn: Boolean,
    firebaseUid: String?,
    user: User?,
    activeCity: String,
    activeLocality: String,
    postPrompt: String?,
    isPosting: Boolean,
    postError: String?,
    skipPostDraftSave: Boolean = false,
    editThoughtId: String?,
    thoughts: List<Thought>,
    pendingDeleteId: String?,
    reportThoughtId: String?,
    onDismissPost: () -> Unit,
    onPostThought: (String, String, String, List<Uri>) -> Unit,
    onDismissEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    onDismissReport: () -> Unit,
    onConfirmReport: (String, String) -> Unit,
) {
    if (showPost && isLoggedIn && firebaseUid != null && user != null && activeCity.isNotBlank()) {
        PostDialog(
            cityLabel = AppCity.displayName(activeCity),
            localityLabel = activeLocality.takeIf { it.isNotBlank() }
                ?.let { AppLocality.displayName(it) },
            onDismiss = onDismissPost,
            promptText = postPrompt,
            isPosting = isPosting,
            postError = postError,
            skipDraftSave = skipPostDraftSave,
            onPostThought = onPostThought,
        )
    }

    val etId = editThoughtId
    val et = remember(etId, thoughts) { etId?.let { id -> thoughts.find { it.id == id } } }
    if (et != null && isLoggedIn && firebaseUid != null && et.authorId == firebaseUid) {
        EditDialog(
            initialText = et.text,
            allowEmptySave = et.imageUrls.isNotEmpty(),
            onDismiss = onDismissEdit,
            onSave = onSaveEdit,
        )
    }

    val deleteId = pendingDeleteId
    val deleteTarget = remember(deleteId, thoughts) {
        deleteId?.let { id -> thoughts.find { it.id == id } }
    }
    if (
        !deleteId.isNullOrBlank() &&
        deleteTarget != null &&
        isLoggedIn &&
        firebaseUid != null &&
        deleteTarget.authorId == firebaseUid
    ) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete thought?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                Button(onClick = { onConfirmDelete(deleteId) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancel")
                }
            },
        )
    }

    val reportId = reportThoughtId
    if (reportId != null && firebaseUid != null) {
        ReportReasonDialog(
            title = "Report post",
            onDismiss = onDismissReport,
            onConfirm = { reason -> onConfirmReport(reportId, reason) },
        )
    }
}
