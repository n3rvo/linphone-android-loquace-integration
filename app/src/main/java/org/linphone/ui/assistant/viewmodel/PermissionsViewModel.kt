package org.linphone.ui.assistant.viewmodel

import android.Manifest
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import org.linphone.core.tools.Log
import org.linphone.ui.GenericViewModel

class PermissionsViewModel
@UiThread
constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Permissions ViewModel]"
    }

    val cameraPermissionGranted = MutableLiveData<Boolean>()

    val recordAudioPermissionGranted = MutableLiveData<Boolean>()

    val readContactsPermissionGranted = MutableLiveData<Boolean>()

    val postNotificationsPermissionGranted = MutableLiveData<Boolean>()

    val callPhonePermissionGranted = MutableLiveData<Boolean>()

    fun setPermissionGranted(permission: String, granted: Boolean) {
        Log.i("$TAG Permission [$permission] is ${if (granted) "granted" else "not granted yet/denied"}")
        when (permission) {
            Manifest.permission.READ_CONTACTS -> readContactsPermissionGranted.postValue(granted)
            Manifest.permission.RECORD_AUDIO -> recordAudioPermissionGranted.postValue(granted)
            Manifest.permission.CAMERA -> cameraPermissionGranted.postValue(granted)
            Manifest.permission.POST_NOTIFICATIONS -> postNotificationsPermissionGranted.postValue(granted)
            Manifest.permission.CALL_PHONE -> callPhonePermissionGranted.postValue(granted)
        }
    }
}