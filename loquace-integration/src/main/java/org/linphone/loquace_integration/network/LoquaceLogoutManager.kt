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
            listOf(".linphonerc", "linphonerc").forEach { name ->
                val file = java.io.File(context.filesDir, name)
                if (file.exists()) file.delete()
            }
            Log.d(TAG, "Linphone config deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete Linphone config: ${e.message}")
        }

        // 4. Clear avatar files
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

        // 5. Delete the whole DB
        try {
            LoquaceDatabase.INSTANCE?.close()
            LoquaceDatabase.INSTANCE = null
            context.deleteDatabase("loquace_db")
            Log.d(TAG, "Database deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete database: ${e.message}")
        }

        // 6. Clear session last
        val sessionManager = SessionManager(context)
        sessionManager.clearSession()
        Log.d(TAG, "Session cleared, token after clear: ${sessionManager.getToken()}")

        Log.d(TAG, "Logout complete")
    }
}