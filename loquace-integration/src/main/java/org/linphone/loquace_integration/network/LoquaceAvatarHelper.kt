package org.linphone.loquace_integration.network

import android.util.Log
import java.io.File

object LoquaceAvatarHelper {

    private const val TAG = "LoquaceAvatarHelper"

    suspend fun fetchAndSaveAvatar(
        contactId: String,
        pictureUrl: String?,
        domain: String,
        token: String,
        userAgent: String,
        filesDir: File
    ): String? {
        if (pictureUrl.isNullOrEmpty()) return null

        val cacheFile = File(filesDir, "avatar_$contactId.jpg")
        if (cacheFile.exists()) return cacheFile.absolutePath

        val bytes = fetchPhoto(domain, token, userAgent, pictureUrl) ?: return null

        return try {
            cacheFile.writeBytes(bytes)
            Log.d(TAG, "Avatar saved for $contactId at ${cacheFile.absolutePath}")
            cacheFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save avatar for $contactId: ${e.message}")
            null
        }
    }

    private suspend fun fetchPhoto(
        domain: String,
        token: String,
        userAgent: String,
        photoPath: String
    ): ByteArray? {
        return try {
            val api = RetrofitClient.createContactsApi(domain)
            val fullUrl = "https://$domain:${LoquaceConfig.PORT}$photoPath"
            api.getContactPhoto(
                url       = fullUrl,
                token     = token,
                userAgent = userAgent,
                tenant    = domain
            ).bytes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch photo at $photoPath: ${e.message}")
            null
        }
    }
}