package com.prod.singles_date.ui.util

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.prod.singles_date.repository.MediaRepository
import java.io.File

class PostImagePickerState internal constructor(
    val selectedImages: List<Uri>,
    val showSourceSheet: Boolean,
    val pickerMessage: String?,
    val onAddPhotoClick: () -> Unit,
    val onDismissSourceSheet: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onChooseGallery: () -> Unit,
    val onRemoveImage: (Uri) -> Unit,
    val onClearAll: () -> Unit,
    val onDismissPickerMessage: () -> Unit,
)

@Composable
fun rememberPostImagePickerState(
    maxImages: Int = MediaRepository.MAX_IMAGES_PER_POST,
    initialImages: List<Uri> = emptyList(),
): PostImagePickerState {
    val context = LocalContext.current
    var selectedImages by remember { mutableStateOf(initialImages.distinct().take(maxImages)) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var pickerMessage by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxImages),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = maxImages - selectedImages.size
            selectedImages = (selectedImages + uris.take(remaining)).distinct()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null && selectedImages.size < maxImages) {
            selectedImages = selectedImages + uri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createCameraCaptureUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            pickerMessage = "Camera permission is needed to take a photo."
        }
    }

    return PostImagePickerState(
        selectedImages = selectedImages,
        showSourceSheet = showSourceSheet,
        pickerMessage = pickerMessage,
        onAddPhotoClick = {
            if (selectedImages.size < maxImages) {
                showSourceSheet = true
            }
        },
        onDismissSourceSheet = { showSourceSheet = false },
        onTakePhoto = {
            showSourceSheet = false
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onChooseGallery = {
            showSourceSheet = false
            val remaining = maxImages - selectedImages.size
            if (remaining > 0) {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }
        },
        onRemoveImage = { uri ->
            selectedImages = selectedImages.filterNot { it == uri }
        },
        onClearAll = { selectedImages = emptyList() },
        onDismissPickerMessage = { pickerMessage = null },
    )
}

private fun createCameraCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

class ProfilePhotoPickerState internal constructor(
    val showSourceSheet: Boolean,
    val pickerMessage: String?,
    val onChangePhotoClick: () -> Unit,
    val onDismissSourceSheet: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onChooseGallery: () -> Unit,
    val onDismissPickerMessage: () -> Unit,
)

@Composable
fun rememberProfilePhotoPickerState(
    onPhotoPicked: (Uri) -> Unit,
): ProfilePhotoPickerState {
    val context = LocalContext.current
    var showSourceSheet by remember { mutableStateOf(false) }
    var pickerMessage by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onPhotoPicked(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) onPhotoPicked(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createCameraCaptureUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            pickerMessage = "Camera permission is needed to take a photo."
        }
    }

    return ProfilePhotoPickerState(
        showSourceSheet = showSourceSheet,
        pickerMessage = pickerMessage,
        onChangePhotoClick = { showSourceSheet = true },
        onDismissSourceSheet = { showSourceSheet = false },
        onTakePhoto = {
            showSourceSheet = false
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onChooseGallery = {
            showSourceSheet = false
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onDismissPickerMessage = { pickerMessage = null },
    )
}
