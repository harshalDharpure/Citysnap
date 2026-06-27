package com.prod.singles_date.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

class MediaRepository(
    private val s3MediaClient: S3MediaClient = S3MediaClient(),
) {
    companion object {
        const val MAX_IMAGES_PER_POST = 4
        const val MAX_IMAGE_UPLOAD_BYTES = 50L * 1024 * 1024
        private const val TAG = "HoghtMedia"
        private const val MAX_EDGE_PX = 2048
        private const val JPEG_QUALITY = 88
    }

    suspend fun uploadThoughtImages(
        context: Context,
        authorId: String,
        thoughtId: String,
        imageUris: List<Uri>,
    ): List<String> {
        val uris = imageUris.take(MAX_IMAGES_PER_POST)
        if (uris.isEmpty()) return emptyList()

        val appContext = context.applicationContext
        val uploadedUrls = mutableListOf<String>()
        try {
            uris.forEachIndexed { index, uri ->
                val compressed = compressImageToFile(appContext, uri)
                try {
                    val publicUrl = s3MediaClient.uploadJpeg(
                        kind = "thought",
                        file = compressed,
                        thoughtId = thoughtId,
                        index = index,
                    )
                    uploadedUrls.add(publicUrl)
                } finally {
                    compressed.delete()
                }
            }
            return uploadedUrls
        } catch (e: Exception) {
            Log.e(TAG, "uploadThoughtImages failed thoughtId=$thoughtId", e)
            deleteThoughtImages(authorId, thoughtId)
            throw e
        }
    }

    suspend fun deleteThoughtImages(authorId: String, thoughtId: String) {
        if (authorId.isBlank() || thoughtId.isBlank()) return
        runCatching {
            s3MediaClient.deletePrefix("thoughts/$authorId/$thoughtId/")
        }.onFailure { e ->
            Log.w(TAG, "deleteThoughtImages failed thoughtId=$thoughtId", e)
        }
    }

    suspend fun uploadProfilePhoto(
        context: Context,
        userId: String,
        imageUri: Uri,
    ): String {
        if (userId.isBlank()) error("Missing user id")
        val appContext = context.applicationContext
        val compressed = compressImageToFile(appContext, imageUri, maxEdgePx = 1280, jpegQuality = 85)
        try {
            return s3MediaClient.uploadJpeg(kind = "profile", file = compressed)
        } finally {
            compressed.delete()
        }
    }

    suspend fun deleteProfilePhoto(userId: String) {
        if (userId.isBlank()) return
        runCatching {
            s3MediaClient.deletePrefix("profiles/$userId/")
        }.onFailure { e ->
            Log.w(TAG, "deleteProfilePhoto failed uid=$userId", e)
        }
    }

    private fun compressImageToFile(
        context: Context,
        uri: Uri,
        maxEdgePx: Int = MAX_EDGE_PX,
        jpegQuality: Int = JPEG_QUALITY,
    ): File {
        val sourceFile = materializeImageFile(context, uri)
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                error("Could not read image")
            }

            val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
                ?: error("Could not read image")

            bitmap = applyExifRotation(bitmap, sourceFile)
            val scaled = scaleDown(bitmap, maxEdgePx)
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled

            val output = File(context.cacheDir, "upload_${System.nanoTime()}.jpg")
            var quality = jpegQuality
            var bytes = compressBitmapToBytes(bitmap, quality)
            while (bytes.size > MAX_IMAGE_UPLOAD_BYTES && quality > 50) {
                quality -= 10
                bytes = compressBitmapToBytes(bitmap, quality)
            }
            bitmap.recycle()

            if (bytes.size > MAX_IMAGE_UPLOAD_BYTES) {
                error("Image is too large after compression. Try a smaller photo.")
            }
            output.writeBytes(bytes)
            return output
        } finally {
            sourceFile.delete()
        }
    }

    private fun materializeImageFile(context: Context, uri: Uri): File {
        val declaredSize = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        if (declaredSize > MAX_IMAGE_UPLOAD_BYTES) {
            error("Each photo must be 50 MB or smaller.")
        }

        val dest = File(context.cacheDir, "upload_src_${System.nanoTime()}.img")
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output, bufferSize = 16 * 1024)
            }
        } ?: run {
            dest.delete()
            error("Could not read image")
        }

        if (copied > MAX_IMAGE_UPLOAD_BYTES) {
            dest.delete()
            error("Each photo must be 50 MB or smaller.")
        }
        if (copied <= 0L) {
            dest.delete()
            error("Could not read image")
        }
        return dest
    }

    private fun applyExifRotation(bitmap: Bitmap, sourceFile: File): Bitmap {
        val rotation = runCatching {
            val exif = ExifInterface(sourceFile.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)

        if (rotation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun compressBitmapToBytes(bitmap: Bitmap, quality: Int): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sampleSize = 1
        val longest = max(width, height)
        while (longest / sampleSize > maxEdge) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val width = source.width
        val height = source.height
        val longest = max(width, height)
        if (longest <= maxEdge) return source

        val scale = maxEdge.toFloat() / longest
        val targetW = (width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
