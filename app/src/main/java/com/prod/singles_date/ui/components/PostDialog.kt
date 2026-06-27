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

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.layout.navigationBarsPadding

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

import androidx.compose.material.icons.filled.CameraAlt

import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.PhotoLibrary

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

import androidx.compose.runtime.DisposableEffect

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.platform.LocalSoftwareKeyboardController

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.unit.dp

import coil.compose.AsyncImage

import com.prod.singles_date.data.LocalPreferences

import com.prod.singles_date.model.PostDraft

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

    skipDraftSave: Boolean = false,

) {

    val context = LocalContext.current

    val prefs = remember { LocalPreferences(context) }

    val savedDraft = remember { prefs.getPostDraft() }

    val restoredImages = remember(savedDraft) {

        savedDraft.imageUris.mapNotNull { uriString ->

            runCatching { Uri.parse(uriString) }.getOrNull()

        }

    }



    val textState = remember { mutableStateOf(savedDraft.text) }

    val postTypeState = remember { mutableStateOf(savedDraft.postType.ifBlank { PostType.SNAP }) }

    val categoryState = remember { mutableStateOf(savedDraft.category) }

    val maxLen = if (postTypeState.value == PostType.NOTE) {

        ThoughtViewModel.MAX_NOTE_LENGTH

    } else {

        ThoughtViewModel.MAX_THOUGHT_LENGTH

    }

    val current = textState.value.take(maxLen)

    val imagePicker = rememberPostImagePickerState(initialImages = restoredImages)

    val isNote = postTypeState.value == PostType.NOTE

    val categoryRequired = isNote && categoryState.value.isBlank()

    val canPost = (current.trim().isNotEmpty() || imagePicker.selectedImages.isNotEmpty()) &&

        !isPosting && !categoryRequired

    val scrollState = rememberScrollState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val focusManager = LocalFocusManager.current

    val keyboardController = LocalSoftwareKeyboardController.current

    val slotsLeft = MediaRepository.MAX_IMAGES_PER_POST - imagePicker.selectedImages.size

    val sheetMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.92f



    val postLabel = when {

        isPosting && imagePicker.selectedImages.isNotEmpty() -> "Posting photos…"

        isPosting -> "Posting…"

        isNote -> "Publish Local Note"

        cityLabel.isNotBlank() -> "Post snap to $cityLabel"

        else -> "Post snap"

    }



    fun persistDraft() {

        prefs.savePostDraft(

            PostDraft(

                text = textState.value,

                category = categoryState.value,

                postType = postTypeState.value,

                imageUris = imagePicker.selectedImages.map { it.toString() },

            ),

        )

    }



    fun requestDismiss() {

        if (!isPosting) {

            if (!skipDraftSave) persistDraft()

            onDismiss()

        }

    }



    DisposableEffect(Unit) {

        onDispose {

            if (!isPosting && !skipDraftSave) persistDraft()

        }

    }



    ModalBottomSheet(

        onDismissRequest = { requestDismiss() },

        sheetState = sheetState,

        containerColor = MaterialTheme.colorScheme.surface,

        dragHandle = { SheetDragHandle() },

    ) {

        Column(

            modifier = Modifier

                .fillMaxWidth()

                .height(sheetMaxHeight)

                .navigationBarsPadding(),

        ) {

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

                    if (savedDraft.hasContent()) {

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(

                            text = "Draft restored — finish and post when ready",

                            style = MaterialTheme.typography.labelSmall,

                            color = MaterialTheme.colorScheme.onSurfaceVariant,

                        )

                    }

                }

                IconButton(onClick = { requestDismiss() }, enabled = !isPosting) {

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

                    .weight(1f, fill = false)

                    .verticalScroll(scrollState)

                    .padding(horizontal = 20.dp)

                    .padding(bottom = 8.dp),

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



                if (!isNote) {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(10.dp),

                    ) {

                        MediaSourceButton(

                            label = "Camera",

                            icon = Icons.Filled.CameraAlt,

                            enabled = !isPosting && slotsLeft > 0,

                            onClick = imagePicker.onTakePhoto,

                            modifier = Modifier.weight(1f),

                        )

                        MediaSourceButton(

                            label = "Gallery",

                            icon = Icons.Filled.PhotoLibrary,

                            enabled = !isPosting && slotsLeft > 0,

                            onClick = imagePicker.onChooseGallery,

                            modifier = Modifier.weight(1f),

                        )

                    }

                    if (slotsLeft < MediaRepository.MAX_IMAGES_PER_POST) {

                        Text(

                            text = "${imagePicker.selectedImages.size}/${MediaRepository.MAX_IMAGES_PER_POST} photos",

                            style = MaterialTheme.typography.labelSmall,

                            color = MaterialTheme.colorScheme.onSurfaceVariant,

                        )

                    }

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

                    minLines = if (isNote) 5 else 3,

                    maxLines = if (isNote) 12 else 8,

                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),

                    keyboardActions = KeyboardActions(

                        onDone = {

                            focusManager.clearFocus()

                            keyboardController?.hide()

                        },

                    ),

                    colors = TextFieldDefaults.colors(

                        focusedTextColor = MaterialTheme.colorScheme.onSurface,

                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,

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



                if (!isNote && imagePicker.selectedImages.isNotEmpty()) {

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

                        if (!isPosting && slotsLeft > 0) {

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

                                    Text("More", style = MaterialTheme.typography.labelSmall)

                                }

                            }

                        }

                    }

                }

            }



            HorizontalDivider()

            Surface(

                color = MaterialTheme.colorScheme.surface,

                shadowElevation = 6.dp,

                modifier = Modifier.fillMaxWidth(),

            ) {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .imePadding()

                        .padding(horizontal = 20.dp, vertical = 16.dp),

                    verticalArrangement = Arrangement.spacedBy(10.dp),

                ) {

                    postError?.let {

                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

                    }

                    GradientCtaButton(

                        text = postLabel,

                        onClick = {

                            onPostThought(current.trim(), categoryState.value, postTypeState.value, imagePicker.selectedImages)

                        },

                        enabled = canPost,

                        loading = isPosting,

                        modifier = Modifier.fillMaxWidth(),

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

            text = { Text("Take a new snap or pick from your gallery.") },

            confirmButton = { TextButton(onClick = imagePicker.onTakePhoto) { Text("Camera") } },

            dismissButton = { TextButton(onClick = imagePicker.onChooseGallery) { Text("Gallery") } },

        )

    }



    val pickerMessage = imagePicker.pickerMessage

    if (pickerMessage != null) {

        AlertDialog(

            onDismissRequest = imagePicker.onDismissPickerMessage,

            title = { Text("Photo access") },

            text = { Text(pickerMessage) },

            confirmButton = {

                TextButton(onClick = imagePicker.onDismissPickerMessage) { Text("OK") }

            },

        )

    }

}



@Composable

private fun MediaSourceButton(

    label: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    enabled: Boolean,

    onClick: () -> Unit,

    modifier: Modifier = Modifier,

) {

    Surface(

        onClick = onClick,

        enabled = enabled,

        shape = RoundedCornerShape(14.dp),

        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),

        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),

        modifier = modifier.height(52.dp),

    ) {

        Row(

            modifier = Modifier.padding(horizontal = 14.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.Center,

        ) {

            Icon(

                imageVector = icon,

                contentDescription = label,

                tint = MaterialTheme.colorScheme.primary,

                modifier = Modifier.size(20.dp),

            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(

                text = label,

                style = MaterialTheme.typography.labelLarge,

                color = MaterialTheme.colorScheme.onSurface,

            )

        }

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


