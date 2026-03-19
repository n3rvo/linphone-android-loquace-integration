package org.linphone.loquace_integration.xmpp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.linphone.loquace_integration.R
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.SessionManager

class XmppConnectionService : Service() {

    companion object {
        private const val TAG = "XmppConnectionService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "loquace_xmpp_channel"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForeground(NOTIFICATION_ID, buildNotification())

        scope.launch {
            if (!LoquaceXmppManager.isConnected()) {
                val context = applicationContext
                val db = LoquaceDatabase.getInstance(context)
                val xmppEntity = db.xmppAccountDao().get()
                if (xmppEntity != null && xmppEntity.enabled) {
                    Log.d(TAG, "Reconnecting XMPP from service")
                    LoquaceXmppManager.connect(xmppEntity)
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Loquace Chat",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Loquace")
            .setContentText("Chat connected")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}