package org.linphone.loquace_integration.sip

import android.content.Context
import android.util.Log
import org.linphone.core.Core
import org.linphone.core.Factory
import org.linphone.core.TransportType
import org.linphone.loquace_integration.storage.entity.SipAccountEntity
import androidx.core.content.edit
import org.linphone.loquace_integration.network.SettingsRepository
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.SessionManager

object LoquaceSipConfigurator {

    private const val TAG = "LoquaceSip"

    fun configure(core: Core, sip: SipAccountEntity, userAgent: String, context: android.content.Context) {
        val factory = Factory.instance()

        Log.d(TAG, "Configuring SIP account for user: ${sip.username} on domain: ${sip.domain}")

        // Set SIP user agent
        core.setUserAgent(userAgent, null)
        Log.d(TAG, "User agent set to: $userAgent")

        val authInfo = factory.createAuthInfo(
            sip.username,
            null,
            sip.password,
            null,
            null,
            sip.domain
        )
        core.addAuthInfo(authInfo)
        Log.d(TAG, "Auth info added")

        val params = core.createAccountParams()

        val identity = factory.createAddress("sip:${sip.username}@${sip.domain}")
        params.identityAddress = identity
        Log.d(TAG, "Identity address: sip:${sip.username}@${sip.domain}")

        val transport = parseTransport(sip.transport)
        val serverAddress = factory.createAddress("sip:${sip.proxy}")?.apply {
            this.transport = transport
        }
        params.serverAddress = serverAddress
        Log.d(TAG, "Server address: sip:${sip.proxy} transport: ${sip.transport}")

        params.isRegisterEnabled = true

        // Enable push notifications
        params.pushNotificationAllowed = true
        params.remotePushNotificationAllowed = true
        Log.d(TAG, "Push notifications enabled for SIP account")

        // Apply pending FCM token BEFORE registering account
        val pendingToken = context.getSharedPreferences("loquace_flags", Context.MODE_PRIVATE)
            .getString("pending_fcm_token", null)
        if (pendingToken != null) {
            try {
                org.linphone.core.tools.AndroidPlatformHelper.instance()?.setPushToken(pendingToken)
                Log.d(TAG, "Applied pending FCM token before registration: $pendingToken")
                context.getSharedPreferences("loquace_flags", Context.MODE_PRIVATE)
                    .edit { remove("pending_fcm_token") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply pending FCM token: ${e.message}")
            }
        }

        // Create and add account
        val account = core.createAccount(params)
        core.addAccount(account)
        core.defaultAccount = account
        Log.d(TAG, "SIP account added and set as default")

        // Set avatar
        val avatarFile = java.io.File(context.filesDir, "my_avatar.jpg")
        if (avatarFile.exists()) {
            val updatedParams = account.params.clone()
            updatedParams.pictureUri = avatarFile.absolutePath
            account.params = updatedParams
            Log.d(TAG, "Avatar set from: ${avatarFile.absolutePath}")
        }
    }

    fun logout(core: Core) {
        Log.d(TAG, "Removing SIP account")
        core.defaultAccount?.let {
            it.params.clone().also { params ->
                params.pushNotificationAllowed = false
                params.remotePushNotificationAllowed = false
                it.params = params
            }
            core.removeAccount(it)
        }
        core.clearAccounts()
        core.clearAllAuthInfo()
        Log.d(TAG, "SIP account removed")
    }

    private fun parseTransport(transport: String): TransportType {
        return when (transport.uppercase()) {
            "TLS"  -> TransportType.Tls
            "TCP"  -> TransportType.Tcp
            "UDP"  -> TransportType.Udp
            else   -> {
                Log.w(TAG, "Unknown transport '$transport', falling back to TCP")
                TransportType.Tcp
            }
        }
    }

    suspend fun checkAndUpdateTransportIfNeeded(core: Core, context: android.content.Context) {
        try {
            val db = LoquaceDatabase.getInstance(context)
            val sessionManager = SessionManager(context)
            val domain = sessionManager.getDomain() ?: return
            val token = sessionManager.getToken() ?: return
            val userAgent = sessionManager.getUserAgent()

            val settingsRepository = SettingsRepository(db)
            settingsRepository.fetchAndStore(domain, token, userAgent, sessionManager)

            val updatedSip = db.sipAccountDao().get() ?: return
            val account = core.defaultAccount ?: return
            val currentTransport = account.params.serverAddress?.transport
            val newTransport = parseTransport(updatedSip.transport)

            if (currentTransport != newTransport) {
                Log.d(TAG, "Transport changed from $currentTransport to $newTransport, updating")
                val params = account.params.clone()
                val newServerAddress = account.params.serverAddress?.clone()
                newServerAddress?.transport = newTransport
                params.serverAddress = newServerAddress
                account.params = params
                account.refreshRegister()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check transport: ${e.message}")
        }
    }
}