package org.linphone.ui.main.dialer.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.core.tools.Log
import org.linphone.loquace_integration.network.CallContactRequest
import org.linphone.loquace_integration.network.CallRequest
import org.linphone.loquace_integration.network.RetrofitClient
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.utils.EmergencyCallUtils
import org.linphone.ui.main.viewmodel.AbstractMainViewModel
import org.linphone.utils.LinphoneUtils

class LoquaceDialerViewModel
@UiThread
constructor() : AbstractMainViewModel() {

    companion object {
        private const val TAG = "[Loquace Dialer ViewModel]"
    }

    val numberInput = MutableLiveData<String>("")

    @UiThread
    fun onDigitClicked(digit: String) {
        numberInput.value = "${numberInput.value.orEmpty()}$digit"
    }

    @UiThread
    fun onZeroLongClicked(): Boolean {
        numberInput.value = "${numberInput.value.orEmpty()}+"
        return true
    }

    @UiThread
    fun onBackspaceClicked() {
        val current = numberInput.value.orEmpty()
        if (current.isNotEmpty()) {
            numberInput.value = current.dropLast(1)
        }
    }

    @UiThread
    fun onBackspaceLongClicked(): Boolean {
        numberInput.value = ""
        return true
    }

    @UiThread
    fun onCallClicked() {
        val number = numberInput.value.orEmpty()
        if (number.isEmpty()) return

        if (EmergencyCallUtils.isEmergencyNumber(coreContext.context, number)) {
            Log.w("$TAG [$number] is an emergency number, placing GSM call instead of SIP")
            EmergencyCallUtils.placeGsmCall(coreContext.context, number)
            numberInput.value = ""
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionManager = SessionManager(coreContext.context)
                val domain = sessionManager.getDomain() ?: return@launch
                val token = sessionManager.getToken() ?: return@launch
                val userAgent = sessionManager.getUserAgent()

                val api = RetrofitClient.createCallsApi(domain)
                val request = CallRequest(contact = CallContactRequest(number = number))
                val response = api.placeCall(token, userAgent, domain, request)

                if (response.failed) {
                    Log.e("$TAG Call request failed for number [$number]")
                    return@launch
                }

                val finalNumber = response.contact.number
                coreContext.postOnCoreThread { core ->
                    val address = core.interpretUrl(
                        finalNumber,
                        LinphoneUtils.applyInternationalPrefix()
                    )
                    if (address != null) {
                        Log.i("$TAG Starting call to [${address.asStringUriOnly()}]")
                        coreContext.startAudioCall(address)
                    } else {
                        Log.e("$TAG Failed to parse [$finalNumber] as SIP address")
                    }
                }

                numberInput.postValue("")
            } catch (e: Exception) {
                Log.e("$TAG Failed to place call: ${e.message}")
            }
        }
    }
}