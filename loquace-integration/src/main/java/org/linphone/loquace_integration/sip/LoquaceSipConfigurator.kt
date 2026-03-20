package org.linphone.loquace_integration.sip

import android.util.Log
import org.linphone.core.Core
import org.linphone.core.Factory
import org.linphone.core.TransportType
import org.linphone.loquace_integration.storage.entity.SipAccountEntity

object LoquaceSipConfigurator {

    private const val TAG = "LoquaceSip"

    fun configure(core: Core, sip: SipAccountEntity, userAgent: String) {
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

        // Create and add account
        val account = core.createAccount(params)
        core.addAccount(account)
        core.defaultAccount = account
        Log.d(TAG, "SIP account added and set as default")
    }

    fun logout(core: Core) {
        Log.d(TAG, "Removing SIP account")
        core.defaultAccount?.let { core.removeAccount(it) }
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
}