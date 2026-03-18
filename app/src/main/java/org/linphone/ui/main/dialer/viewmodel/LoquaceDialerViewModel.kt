package org.linphone.ui.main.dialer.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.core.tools.Log
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

        coreContext.postOnCoreThread { core ->
            val address = core.interpretUrl(
                number,
                LinphoneUtils.applyInternationalPrefix()
            )
            if (address != null) {
                Log.i("$TAG Starting call to [${address.asStringUriOnly()}]")
                coreContext.startAudioCall(address)
                numberInput.postValue("")
            } else {
                Log.e("$TAG Failed to parse [$number] as SIP address")
            }
        }
    }
}