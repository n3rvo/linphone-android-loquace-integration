package org.linphone.loquace_integration.viewmodel

import android.util.Log
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import org.linphone.loquace_integration.network.LoginRequest
import org.linphone.loquace_integration.network.RetrofitClient
import org.linphone.loquace_integration.network.SettingsRepository
import org.linphone.loquace_integration.network.PresenceRepository
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.linphone.loquace_integration.network.LoquaceMediaDownloader
import org.linphone.loquace_integration.sip.LoquaceCoreProvider
import org.linphone.loquace_integration.sip.LoquaceSipConfigurator
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager
import org.linphone.loquace_integration.xmpp.XmppConnectionService
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LoquaceLoginViewModel(
    private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(domain: String, username: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                // Build user agent once, reuse everywhere
                val userAgent = buildUserAgent(context)
                sessionManager.saveUserAgent(userAgent)
                Log.d(TAG, "User-Agent: $userAgent")

                Log.d(TAG, "Fetching FCM token...")
                val fcmToken = getFcmToken()
                Log.d(TAG, "FCM token retrieved")

                Log.d(TAG, "Attempting login for user '$username' on domain '$domain'")
                val api = RetrofitClient.createAuthApi(domain)
                val response = api.login(
                    tenant          = domain,
                    mobilePushToken = fcmToken,
                    pushToken       = fcmToken,
                    userAgent       = userAgent,
                    request         = LoginRequest(username, password)
                )

                Log.d(TAG, "Login successful, token received")
                sessionManager.saveToken(response.token)
                sessionManager.saveDomain(domain)

                val db = LoquaceDatabase.getInstance(context)

                Log.d(TAG, "Fetching initial settings...")
                val settingsRepository = SettingsRepository(db)
                settingsRepository.fetchAndStore(domain, response.token, userAgent, sessionManager)
                val avatarUrl = sessionManager.getAvatarUrl()
                if (!avatarUrl.isNullOrEmpty()) {
                    val bytes = LoquaceMediaDownloader.downloadBytes(
                        url    = avatarUrl,
                        token  = response.token,
                        domain = domain
                    )
                    if (bytes != null) {
                        val avatarFile = File(context.filesDir, "my_avatar.jpg")
                        avatarFile.writeBytes(bytes)
                    }
                }

                Log.d(TAG, "Fetching initial presence...")
                val presenceRepository = PresenceRepository(db)
                presenceRepository.fetchAndStore(domain, response.token, userAgent)

                Log.d(TAG, "Login flow completed successfully")

                Log.d(TAG, "Configuring SIP account...")
                val sipEntity = db.sipAccountDao().get()
                if (sipEntity != null) {
                    LoquaceSipConfigurator.configure(LoquaceCoreProvider.getCore(), sipEntity, userAgent, context)
                    Log.d(TAG, "SIP account configured successfully")
                } else {
                    Log.e(TAG, "SIP account data not found in database")
                }

                // Start XMPP connection
                val xmppEntity = db.xmppAccountDao().get()
                if (xmppEntity != null && xmppEntity.enabled) {
                    Log.d(TAG, "Starting XMPP connection for ${xmppEntity.username}@${xmppEntity.domain}")
                    LoquaceXmppManager.connect(xmppEntity, userAgent)
                    val serviceIntent = Intent(context, XmppConnectionService::class.java)
                    context.startForegroundService(serviceIntent)
                    Log.d(TAG, "XMPP connection initiated and service started")
                } else {
                    Log.w(TAG, "XMPP account not available or not enabled, skipping")
                }

                _state.value = LoginState.Success

            } catch (e: Exception) {
                Log.e(TAG, "Login flow failed: ${e.message}", e)
                _state.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    private suspend fun getFcmToken(): String = suspendCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    private fun buildUserAgent(context: Context): String {
        val appName = context.getString(R.string.app_name_agent)
        val osVersion = Build.VERSION.RELEASE
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val deviceModel = Build.MODEL
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return "$appName/Android-$osVersion/$versionName/$deviceModel/$androidId"
    }

    companion object {
        private const val TAG = "LoquaceLogin"
    }
}

sealed class LoginState {
    object Idle    : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}