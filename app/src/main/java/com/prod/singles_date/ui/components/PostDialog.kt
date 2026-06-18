package com.prod.singles_date.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prod.singles_date.model.PostType
import com.prod.singles_date.model.ThoughtCategory
import com.prod.singles_date.repository.MediaRepository
import com.prod.singles_date.ui.util.rememberPostImagePickerState
import com.prod.singles_date.viewmodel.ThoughtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDialog(
    cityLabel: String,
    localityLabel: String?,
    onDismiss: () -> Unit,
    onPostThought: (text: String, category: String, postType: String, imageUris: List<Uri>) -> Unit,
    promptText: String? = null,
    isPosting: Boolean = false,
    postError: String? = null,
) {
    val textState = remember { mutableStateOf("") }
    val postTypeState = remember { mutableStateOf(PostType.SNAP) }
    val categoryState = remember { mutableStateOf("") }
    val maxLen = if (postTypeState.value == PostType.NOTE) {
        ThoughtViewModel.MAX_NOTE_LENGTH
    } else {
        ThoughtViewModel.MAX_THOUGHT_LENGTH
    }
    val current = textState.value.take(maxLen)
    val imagePicker = rememberPostImagePickerState()
    val isNote = postTypeState.value == PostType.NOTE
    val categoryRequired = isNote && categoryState.value.isBlank()
    val canPost = (current.trim().isNotEmpty() || imagePicker.selectedImages.isNotEmpty()) &&
        !isPosting && !categoryRequired
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val postLabel = when {
        isPosting && imagePicker.selectedImages.isNotEmpty() -> "Posting photos…"
        isPosting -> "Posting…"
        isNote -> "Publish Local Note"
        cityLabel.isNotBlank() -> "Post snap to $cityLabel"
        else -> "Post snap"
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isPosting) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { SheetDragHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNote) "Write a Local Note" else "Share a city moment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (cityLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        ) {
                            Text(
                                text = buildString {
                                    append(cityLabel)
                                    if (!localityLabel.isNullOrBlank()) append(" · $localityLabel")
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss, enabled = !isPosting) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = !isNote,
                    onClick = { postTypeState.value = PostType.SNAP },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Snap") }
                SegmentedButton(
                    selected = isNote,
                    onClick = {
                        postTypeState.value = PostType.NOTE
                        imagePicker.onClearAll()
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Local Note") }
            }

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThoughtCategory.ALL.forEach { cat ->
                        FilterChip(
                            selected = categoryState.value == cat,
                            onClick = { categoryState.value = if (categoryState.value == cat) "" else cat },
                            label = {
                                Text("${ThoughtCategory.emoji(cat)} ${ThoughtCategory.displayName(cat)}")
                            },
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }
                if (categoryRequired) {
                    Text(
                        "Pick a category for Local Notes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                TextField(
                    value = current,
                    onValueChange = { textState.value = it.take(maxLen) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            when {
                                isNote -> "Jobs, rent reality, startup layoffs — longer local stories welcome."
                                else -> promptText ?: "Traffic, food, rent, vibes — keep it local."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    enabled = !isPosting,
                    minLines = if (isNote) 6 else 4,
                    maxLines = if (isNote) 14 else 8,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canPost) {
                                onPostThought(current.trim(), categoryState.value, postTypeState.value, imagePicker.selectedImages)
                            }
                        },
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )

                Text(
                    text = "${current.length}/$maxLen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )

                if (!isNote) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        imagePicker.selectedImages.forEach { uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                if (!isPosting) {
                                    IconButton(
                                        onClick = { imagePicker.onRemoveImage(uri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                        if (!isPosting && imagePicker.selectedImages.size < MediaRepository.MAX_IMAGES_PER_POST) {
                            Surface(
                                onClick = imagePicker.onAddPhotoClick,
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Photo", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                postError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = onDismiss, enabled = !isPosting) { Text("Cancel") }
                    GradientCtaButton(
                        text = postLabel,
                        onClick = {
                            onPostThought(current.trim(), categoryState.value, postTypeState.value, imagePicker.selectedImages)
                        },
                        enabled = canPost,
                        loading = isPosting,
                        modifier = Modifier.weight(1f),
                        trailingContent = {
                            if (!isPosting) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                    )
                }
            }
        }
    }

    if (imagePicker.showSourceSheet) {
        AlertDialog(
            onDismissRequest = imagePicker.onDismissSourceSheet,
            title = { Text("Add a photo") },
            confirmButton = { TextButton(onClick = imagePicker.onTakePhoto) { Text("Camera") } },
            dismissButton = { TextButton(onClick = imagePicker.onChooseGallery) { Text("Gallery") } },
        )
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
    )
}
