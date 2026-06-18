package com.prod.singles_date.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.FileProvider
import com.prod.singles_date.model.AppCity
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun shareThoughtAsText(context: Context, thoughtText: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$thoughtText — from Citysnap")
    }
    context.startActivity(Intent.createChooser(send, null))
}

fun shareThoughtAsImage(
    context: Context,
    thoughtText: String,
    authorName: String,
    cityId: String,
    feelCount: Int,
    imageUrl: String? = null,
) {
    runCatching {
        val photo = imageUrl?.takeIf { it.isNotBlank() }?.let { loadBitmapFromUrl(it) }
        val bitmap = renderThoughtCard(
            text = thoughtText,
            authorName = authorName,
            cityId = cityId,
            feelCount = feelCount,
            photoBitmap = photo,
        )
        photo?.recycle()
        val uri = saveCardToCache(context, bitmap)
        bitmap.recycle()

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, thoughtText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share thought"))
    }.onFailure { t ->
        Log.e("CitysnapShare", "Image share failed, falling back to text", t)
        shareThoughtAsText(context, thoughtText)
    }
}

/** Share a weekly city mood summary card (9:16). */
fun shareCityMoodCard(
    context: Context,
    cityId: String,
    moodLines: List<String>,
) {
    runCatching {
        val bitmap = renderMoodCard(cityId, moodLines)
        val uri = saveCardToCache(context, bitmap)
        bitmap.recycle()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share city mood"))
    }.onFailure {
        Log.e("CitysnapShare", "Mood card share failed", it)
    }
}

private const val CARD_WIDTH = 1080
private const val CARD_HEIGHT = 1920
private const val SIDE_MARGIN = 100f

private fun renderThoughtCard(
    text: String,
    authorName: String,
    cityId: String,
    feelCount: Int,
    photoBitmap: Bitmap? = null,
): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawNightBackground(canvas)

    val cityName = AppCity.displayName(cityId).uppercase()
    val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 42f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        letterSpacing = 0.15f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("━━━━━━━━━━", CARD_WIDTH / 2f, 280f, headerPaint)
    canvas.drawText(cityName, CARD_WIDTH / 2f, 360f, headerPaint)

    if (authorName.isNotBlank()) {
        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C8C8C8")
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(authorName, CARD_WIDTH / 2f, 430f, authorPaint)
    }

    var contentTop = 500f
    photoBitmap?.let { photo ->
        val maxPhotoHeight = 720f
        val scale = minOf(
            (CARD_WIDTH - SIDE_MARGIN * 2) / photo.width.toFloat(),
            maxPhotoHeight / photo.height.toFloat(),
        )
        val w = photo.width * scale
        val h = photo.height * scale
        val left = (CARD_WIDTH - w) / 2f
        val dest = android.graphics.RectF(left, contentTop, left + w, contentTop + h)
        canvas.drawBitmap(photo, null, dest, null)
        contentTop = dest.bottom + 40f
    }

    val contentWidth = (CARD_WIDTH - SIDE_MARGIN * 2).toInt()
    if (text.isNotBlank()) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F5F5")
            textSize = if (text.length > 120) 64f else 76f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(10f, 1f)
            .build()

        val textTop = if (photoBitmap != null) {
            contentTop
        } else {
            (CARD_HEIGHT - textLayout.height) / 2f - 40f
        }
        canvas.save()
        canvas.translate(SIDE_MARGIN, textTop)
        textLayout.draw(canvas)
        canvas.restore()
    }

    val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0B0B0")
        textSize = 40f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("❤️ $feelCount feel this", CARD_WIDTH / 2f, CARD_HEIGHT - 280f, metaPaint)

    val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F5F5")
        textSize = 56f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("CITYSNAP", CARD_WIDTH / 2f, CARD_HEIGHT - 180f, brandPaint)

    val taglinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6E6E6E")
        textSize = 32f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Real thoughts. Real cities. Real people.", CARD_WIDTH / 2f, CARD_HEIGHT - 120f, taglinePaint)
    canvas.drawText("━━━━━━━━━━", CARD_WIDTH / 2f, CARD_HEIGHT - 70f, headerPaint)

    return bitmap
}

private fun renderMoodCard(cityId: String, moodLines: List<String>): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawNightBackground(canvas)

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F5F5")
        textSize = 56f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("${AppCity.displayName(cityId)} Mood This Week", CARD_WIDTH / 2f, 360f, titlePaint)

    val linePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D0D0D0")
        textSize = 48f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    var y = 520f
    moodLines.forEach { line ->
        canvas.drawText(line, CARD_WIDTH / 2f, y, linePaint)
        y += 80f
    }

    val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F5F5")
        textSize = 52f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("CITYSNAP", CARD_WIDTH / 2f, CARD_HEIGHT - 140f, brandPaint)

    return bitmap
}

private fun drawNightBackground(canvas: Canvas) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
            intArrayOf(
                Color.parseColor("#0A0A0A"),
                Color.parseColor("#161622"),
                Color.parseColor("#0A0A0A"),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)
}

private fun saveCardToCache(context: Context, bitmap: Bitmap): Uri {
    val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
    val file = File(dir, "thought_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun loadBitmapFromUrl(url: String): Bitmap? = runBlocking {
    withContext(Dispatchers.IO) {
        runCatching {
            URL(url).openStream().use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }
}
