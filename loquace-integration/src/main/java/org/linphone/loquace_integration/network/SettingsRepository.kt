package org.linphone.loquace_integration.network

import android.util.Log
import com.google.gson.Gson
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.storage.entity.*

class SettingsRepository(private val db: LoquaceDatabase) {

    private val gson = Gson()

    suspend fun fetchAndStore(domain: String, token: String, userAgent: String, sessionManager: SessionManager) {
        Log.d(TAG, "Fetching settings for domain: $domain")
        val api = RetrofitClient.createSettingsApi(domain)
        val response = api.getSettings(
            token     = token,
            userAgent = userAgent,
            tenant    = domain
        )
        response.profile.avatarUrl?.let { sessionManager.saveAvatarUrl(it) }
        Log.d(TAG, "Settings received for user: ${response.profile.firstName} ${response.profile.lastName}")
        Log.d(TAG, "SIP domain: ${response.sipAccount.domain}")
        Log.d(TAG, "SIP username: ${response.sipAccount.username}")
        Log.d(TAG, "XMPP enabled: ${response.xmppAccount.enabled}")
        storeSettings(response)
        Log.d(TAG, "Settings stored successfully")
    }

    suspend fun fetchAndStoreCalls(domain: String, token: String, userAgent: String) {
        Log.d(TAG, "Fetching calls settings for domain: $domain")
        val api = RetrofitClient.createSettingsApi(domain)
        val response = api.getCallsSettings(
            token     = token,
            userAgent = userAgent,
            tenant    = domain
        )
        Log.d(TAG, "Calls settings received, devices count: ${response.inbound.devices.size}")
        db.callsDao().save(
            CallsEntity(inboundDevices = gson.toJson(response.inbound.devices))
        )
        Log.d(TAG, "Calls settings stored successfully")
    }

    companion object {
        private const val TAG = "LoquaceSettings"
    }

    private suspend fun storeSettings(response: SettingsResponse) {
        db.sipAccountDao().save(
            SipAccountEntity(
                username  = response.sipAccount.username,
                password  = response.sipAccount.password,
                domain    = response.sipAccount.domain,
                proxy     = response.sipAccount.proxy,
                transport = response.sipAccount.transport,
                wsServers = gson.toJson(response.sipAccount.wsServers),
                codecs    = gson.toJson(response.sipAccount.audio.codecs)
            )
        )
        db.profileDao().save(
            ProfileEntity(
                firstName            = response.profile.firstName,
                lastName             = response.profile.lastName,
                email                = response.profile.email,
                organization         = response.profile.organization,
                organizationalUnit   = response.profile.organizationalUnit,
                role                 = response.profile.role,
                avatarUrl            = response.profile.avatarUrl,
                phones               = gson.toJson(response.profile.phones)
            )
        )
        db.xmppAccountDao().save(
            XmppAccountEntity(
                enabled       = response.xmppAccount.enabled,
                username      = response.xmppAccount.username,
                password      = response.xmppAccount.password,
                domain        = response.xmppAccount.domain,
                serverAddress = response.xmppAccount.serverAddress,
                serverPort    = response.xmppAccount.serverPort
            )
        )
        db.callsDao().save(
            CallsEntity(
                inboundDevices = gson.toJson(response.calls.inbound.devices)
            )
        )
    }
}