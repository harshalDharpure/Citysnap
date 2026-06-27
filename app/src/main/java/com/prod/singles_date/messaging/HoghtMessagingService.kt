package com.prod.singles_date.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.prod.singles_date.MainActivity
import com.prod.singles_date.R
import com.prod.singles_date.util.AppLinks

class HoghtMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: data["body"] ?: "Someone interacted with your thought."
        showNotification(
            title = title,
            body = body,
            type = data["type"].orEmpty(),
            thoughtId = data["thoughtId"].orEmpty(),
            promptText = data["promptText"].orEmpty(),
            conversationId = data["conversationId"].orEmpty(),
            senderId = data["senderId"].orEmpty(),
        )
    }

    private fun saveToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        thoughtId: String,
        promptText: String,
        conversationId: String,
        senderId: String,
    ) {
        ensureChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationIntentParser.EXTRA_TYPE, type)
            putExtra(NotificationIntentParser.EXTRA_PROMPT, promptText)
            if (conversationId.isNotBlank() && senderId.isNotBlank()) {
                putExtra(NotificationIntentParser.EXTRA_CONVERSATION_ID, conversationId)
                putExtra(NotificationIntentParser.EXTRA_SENDER_ID, senderId)
            }
            if (thoughtId.isNotBlank()) {
                putExtra(NotificationIntentParser.EXTRA_THOUGHT_ID, thoughtId)
                data = Uri.parse(AppLinks.thoughtUrl(thoughtId))
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            (thoughtId + promptText + conversationId).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "citysnap_default"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Reactions, replies, messages, trending thoughts, daily prompts, and locality topics."
                }
                context.getSystemService(NotificationManager::class.java)
                    ?.createNotificationChannel(channel)
            }
        }
    }
}
