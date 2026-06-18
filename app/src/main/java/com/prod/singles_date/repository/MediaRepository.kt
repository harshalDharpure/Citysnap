package com.prod.singles_date.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class MediaRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {
    companion object {
        const val MAX_IMAGES_PER_POST = 4
        private const val TAG = "HoghtMedia"
        private const val MAX_EDGE_PX = 1280
        private const val JPEG_QUALITY = 80
    }

    suspend fun uploadThoughtImages(
        context: Context,
        authorId: String,
        thoughtId: String,
        imageUris: List<Uri>,
    ): List<String> {
        val uris = imageUris.take(MAX_IMAGES_PER_POST)
        if (uris.isEmpty()) return emptyList()

        try {
            uris.forEachIndexed { index, uri ->
                val bytes = compressImage(context, uri)
                val ref = storage.reference.child("thoughts/$authorId/$thoughtId/$index.jpg")
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()
                ref.putBytes(bytes, metadata).await()
            }
            return uris.indices.map { index ->
                storage.reference.child("thoughts/$authorId/$thoughtId/$index.jpg")
                    .downloadUrl
                    .await()
                    .toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadThoughtImages failed thoughtId=$thoughtId", e)
            deleteThoughtImages(authorId, thoughtId)
            throw e
        }
    }

    suspend fun deleteThoughtImages(authorId: String, thoughtId: String) {
        if (authorId.isBlank() || thoughtId.isBlank()) return
        runCatching {
            val folder = storage.reference.child("thoughts/$authorId/$thoughtId")
            val listing = folder.listAll().await()
            listing.items.forEach { it.delete().await() }
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
        val bytes = compressImage(context, imageUri)
        val ref = storage.reference.child("profiles/$userId/avatar.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(bytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteProfilePhoto(userId: String) {
        if (userId.isBlank()) return
        runCatching {
            storage.reference.child("profiles/$userId/avatar.jpg").delete().await()
        }.onFailure { e ->
            Log.w(TAG, "deleteProfilePhoto failed uid=$userId", e)
        }
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: error("Could not read image")

        val scaled = scaleDown(bitmap, MAX_EDGE_PX)
        if (scaled !== bitmap) bitmap.recycle()

        return ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            scaled.recycle()
            stream.toByteArray()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sampleSize = 1
        val longest = max(width, height)
        while (longest / sampleSize > maxEdge * 2) {
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
