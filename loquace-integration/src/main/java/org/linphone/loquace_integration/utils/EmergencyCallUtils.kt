package org.linphone.loquace_integration.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat

object EmergencyCallUtils {

    fun isEmergencyNumber(context: Context, number: String): Boolean {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                telephonyManager.isEmergencyNumber(number)
            } else {
                @Suppress("DEPRECATION")
                PhoneNumberUtils.isEmergencyNumber(number)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun placeGsmCall(context: Context, number: String) {
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasPermission) {
            Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$number") }
        } else {
            Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$number") }
        }

        // Try to target the system dialer directly to avoid the app chooser
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialerPackage = telecomManager?.defaultDialerPackage
        if (!defaultDialerPackage.isNullOrEmpty()) {
            intent.setPackage(defaultDialerPackage)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback if the targeted package can't handle it for some reason
            intent.setPackage(null)
            context.startActivity(intent)
        }
    }
}