package com.prod.singles_date.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.util.AppLinks

fun formatThoughtShareMessage(
    thoughtText: String,
    thoughtId: String,
    cityId: String,
): String {
    val cityName = if (cityId.isNotBlank()) AppCity.displayName(cityId) else "your city"
    return buildString {
        appendLine("$cityName Thought")
        appendLine()
        appendLine("\"$thoughtText\"")
        appendLine()
        append("Read more:\n")
        append(AppLinks.thoughtUrl(thoughtId))
    }
}

fun copyThoughtToClipboard(context: Context, thoughtText: String, thoughtId: String, cityId: String) {
    val message = formatThoughtShareMessage(thoughtText, thoughtId, cityId)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Citysnap thought", message))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun shareThoughtAsText(context: Context, thoughtText: String, thoughtId: String, cityId: String) {
    val message = formatThoughtShareMessage(thoughtText, thoughtId, cityId)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(send, "Share thought"))
}

fun shareThoughtToWhatsApp(context: Context, thoughtText: String, thoughtId: String, cityId: String) {
    val message = formatThoughtShareMessage(thoughtText, thoughtId, cityId)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        setPackage("com.whatsapp")
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        shareThoughtAsText(context, thoughtText, thoughtId, cityId)
    }
}

fun formatInviteMessage(referralCode: String): String {
    return buildString {
        appendLine("Join me on Citysnap — discover what your city is really thinking.")
        appendLine("Real thoughts. Real cities. Real people.")
        append(AppLinks.inviteUrl(referralCode))
    }
}

fun copyInviteLink(context: Context, referralCode: String) {
    val message = formatInviteMessage(referralCode)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Citysnap invite", message))
    Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show()
}

fun shareInviteLink(context: Context, referralCode: String) {
    val message = formatInviteMessage(referralCode)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(send, "Invite friends"))
}
