package org.linphone.loquace_integration.xmpp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.SessionManager

class XmppConnectionService : Service() {

    companion object {
        private const val TAG = "XmppConnectionService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userAgent = intent?.getStringExtra("userAgent")?.ifEmpty { null }
            ?: SessionManager(applicationContext).getUserAgent()

        Log.d(TAG, "Service started")

        scope.launch {
            if (!LoquaceXmppManager.isConnected()) {
                val db = LoquaceDatabase.getInstance(applicationContext)
                val xmppEntity = db.xmppAccountDao().get()
                if (xmppEntity != null && xmppEntity.enabled) {
                    Log.d(TAG, "Reconnecting XMPP from service")
                    LoquaceXmppManager.connect(xmppEntity, userAgent)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}