package org.linphone.ui.main.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.tools.Log
import org.linphone.loquace_integration.network.RetrofitClient
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.ui.GenericViewModel
import org.linphone.utils.Event
import android.content.Context
import org.linphone.LinphoneApplication
import org.linphone.loquace_integration.network.CallsResponse
import org.linphone.loquace_integration.network.Device
import org.linphone.loquace_integration.network.Inbound
import androidx.core.content.edit

class LoquaceDrawerMenuViewModel
@UiThread
constructor() : GenericViewModel() {

    companion object {
        private const val TAG = "[Loquace Drawer Menu ViewModel]"
    }

    // Presence
    val presenceStatus = MutableLiveData<String>("ONLINE")
    val presenceMessage = MutableLiveData<String>("")
    val presenceName = MutableLiveData<String>("")

    // Account Info
    val sipAccount = MutableLiveData<String>("")
    val sipNumber = MutableLiveData<String>("")
    val isNetworkAvailable = MutableLiveData<Boolean>(true)
    val callsSettingsSubmittedEvent = MutableLiveData<Event<Boolean>>()

    // Incoming calls devices
    val mobileEnabled = MutableLiveData<Boolean>(false)
    val browserEnabled = MutableLiveData<Boolean>(false)
    val phoneEnabled = MutableLiveData<Boolean>(false)

    val isLoading = MutableLiveData<Boolean>(false)

    val languageChangedEvent = MutableLiveData<Event<String>>()

    val closeDrawerEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val logoutEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    fun fetchData(context: Context) {
        val sessionManager = SessionManager(context)
        val domain = sessionManager.getDomain() ?: return
        val token = sessionManager.getToken() ?: return
        val userAgent = sessionManager.getUserAgent()

        viewModelScope.launch {
            isLoading.value = true
            try {
                // Fetch presence
                withContext(Dispatchers.IO) {
                    try {
                        val presenceApi = RetrofitClient.createPresenceApi(domain)
                        val presence = presenceApi.getPresence(token, userAgent, domain)
                        presenceStatus.postValue(presence.status ?: "ONLINE")
                        presenceMessage.postValue(presence.message ?: "")
                        presenceName.postValue(presence.name ?: "")
                        Log.d(TAG, "Presence fetched: ${presence.status}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch presence: ${e.message}")
                    }
                }

                // Fetch incoming calls devices
                withContext(Dispatchers.IO) {
                    try {
                        val settingsApi = RetrofitClient.createSettingsApi(domain)
                        val calls = settingsApi.getCallsSettings(token, userAgent, domain)
                        for (device in calls.inbound.devices) {
                            when (device.type) {
                                "mobile" -> mobileEnabled.postValue(device.enabled)
                                "browser" -> browserEnabled.postValue(device.enabled)
                                "phone" -> phoneEnabled.postValue(device.enabled)
                            }
                        }
                        Log.d(TAG, "Calls settings fetched")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch calls settings: ${e.message}")
                    }
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    // Presence - called on submit button click
    fun submitPresence(context: Context) {
        val sessionManager = SessionManager(context)
        val domain = sessionManager.getDomain() ?: return
        val token = sessionManager.getToken() ?: return
        val userAgent = sessionManager.getUserAgent()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val presenceApi = RetrofitClient.createPresenceApi(domain)
                presenceApi.updatePresence(
                    token     = token,
                    userAgent = userAgent,
                    tenant    = domain,
                    body      = mapOf(
                        "status"  to (presenceStatus.value ?: "ONLINE"),
                        "message" to (presenceMessage.value ?: "")
                    )
                )
                Log.d(TAG, "Presence submitted: status=${presenceStatus.value}, message=${presenceMessage.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit presence: ${e.message}")
            }
        }
    }

    // Calls - called on submit button click
    fun submitCallsSettings(context: Context) {
        val sessionManager = SessionManager(context)
        val domain = sessionManager.getDomain() ?: return
        val token = sessionManager.getToken() ?: return
        val userAgent = sessionManager.getUserAgent()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settingsApi = RetrofitClient.createSettingsApi(domain)
                val devices = listOf(
                    Device(enabled = mobileEnabled.value ?: false, type = "mobile"),
                    Device(enabled = browserEnabled.value ?: false, type = "browser"),
                    Device(enabled = phoneEnabled.value ?: false, type = "phone")
                )
                settingsApi.updateCallsSettings(
                    token     = token,
                    userAgent = userAgent,
                    tenant    = domain,
                    body      = CallsResponse(inbound = Inbound(devices = devices))
                )
                Log.d(TAG, "Calls settings submitted")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit calls settings: ${e.message}")
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ... existing API call ...
                Log.d(TAG, "Calls settings submitted")
                callsSettingsSubmittedEvent.postValue(Event(true))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit calls settings: ${e.message}")
            }
        }
    }

    fun logout() {
        logoutEvent.value = Event(true)
    }

    fun onLanguageClicked() {
        // Remove navigation to settings, handle language toggle here instead
    }

    fun setLanguage(language: String) {
        val context = LinphoneApplication.coreContext.context
        context.getSharedPreferences("loquace_preferences", Context.MODE_PRIVATE)
            .edit { putString("language", language) }
        languageChangedEvent.postValue(Event(language))
    }

    fun setSipInfo(username: String, domain: String) {
        sipNumber.postValue(username)
        sipAccount.postValue("$username@$domain")
    }
}