package org.linphone.core

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.core.tools.AndroidPlatformHelper
import org.linphone.core.tools.compatibility.DeviceUtils
import org.linphone.ui.main.MainActivity
import androidx.core.content.edit


class LoquaceFirebaseMessagingService : org.linphone.core.tools.firebase.FirebaseMessaging() {

    companion object {
        private const val TAG = "LoquaceFCM"
    }

    override fun onNewToken(token: String) {
        Log.d("LoquaceSip", "New FCM token: $token")
        applicationContext.getSharedPreferences("loquace_flags", Context.MODE_PRIVATE)
            .edit { putString("pending_fcm_token", token) }

        // Verify it was stored
        val stored = applicationContext.getSharedPreferences("loquace_flags", Context.MODE_PRIVATE)
            .getString("pending_fcm_token", null)
        Log.d("LoquaceSip", "Token stored successfully: ${stored != null}, value: $stored")

        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val callId = data["call-id"] ?: ""
        val subject = data["subject"]

        Log.d("LoquaceSip", "Push received! Data: $data")
        Log.d(TAG, "Push received with keepAlive service running: ${coreContext.core.config.getBool("app", "keep_service_alive", false)}")

        when {
            !callId.isNullOrEmpty() -> {
                Log.d("LoquaceSip", "SIP push with call-id: $callId")
                // Force re-registration to ensure FreeSWITCH has current contact
                try {
                    coreContext.core.defaultAccount?.refreshRegister()
                    Log.d("LoquaceSip", "Refreshed SIP registration on push received")
                } catch (e: Exception) {
                    Log.e("LoquaceSip", "Failed to refresh registration: ${e.message}")
                }
                super.onMessageReceived(message)
            }
            subject == "chat" -> {
                Log.d(TAG, "Chat push from ${data["senderJid"]}: ${data["body"]}")
                showChatNotification(data)
            }
            else -> {
                Log.d("LoquaceSip", "SIP call push detected, forwarding to Linphone")
                super.onMessageReceived(message)
            }
        }
    }

    private fun showChatNotification(data: Map<String, String>) {
        val senderName = data["title"] ?: "New message"
        val body = data["body"] ?: ""
        val senderJid = data["senderJid"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "loquace_chat_notifications"
        createNotificationChannel(channelId)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(org.linphone.R.drawable.loquace_logo)
            .setContentTitle(senderName)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(senderJid.hashCode(), notification)
        Log.d(TAG, "Chat notification shown for $senderName")
    }

    private fun createNotificationChannel(channelId: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Chat Messages",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming chat messages"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}