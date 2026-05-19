package org.linphone.loquace_integration.network

import android.content.Context
import android.util.Log
import org.linphone.loquace_integration.sip.LoquaceCoreProvider
import org.linphone.loquace_integration.sip.LoquaceSipConfigurator
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager

object LoquaceLogoutManager {

    private const val TAG = "LoquaceLogoutManager"

    suspend fun logout(context: Context) {
        Log.d(TAG, "Starting logout process")

        // 1. Disconnect XMPP and clear state
        LoquaceXmppManager.logout()
        Log.d(TAG, "XMPP disconnected")

        // 2. Remove SIP account from Linphone Core
        try {
            LoquaceSipConfigurator.logout(LoquaceCoreProvider.getCore())
            Log.d(TAG, "SIP account removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove SIP account: ${e.message}")
        }

        // 3. Clear Linphone config files
        try {
            val linphoneRc = java.io.File(context.filesDir, ".linphonerc")
            if (linphoneRc.exists()) {
                linphoneRc.delete()
                Log.d(TAG, "Linphone config deleted")
            }
            val linphoneRcFactory = java.io.File(context.filesDir, "linphonerc")
            if (linphoneRcFactory.exists()) {
                linphoneRcFactory.delete()
                Log.d(TAG, "Linphone factory config deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete Linphone config: ${e.message}")
        }

        // 4. Clear DB conversations
        try {
            val db = LoquaceDatabase.getInstance(context)
            db.xmppConversationDao().deleteAll()
            Log.d(TAG, "Conversations cleared from DB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear DB: ${e.message}")
        }

        // 5. Clear avatar files
        try {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("avatar_") || file.name == "my_avatar.jpg") {
                    file.delete()
                }
            }
            Log.d(TAG, "Avatar files cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear avatars: ${e.message}")
        }

        // 6. Clear session last (token, domain, etc.)
        val sessionManager = SessionManager(context)
        sessionManager.clearSession()
        Log.d(TAG, "Session cleared")

        Log.d(TAG, "Logout complete")
    }
}