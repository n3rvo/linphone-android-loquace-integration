package org.linphone.loquace_integration.network

import android.util.Log
import okhttp3.ResponseBody

class LoquaceContactsRepository {

    companion object {
        private const val TAG = "LoquaceContacts"
        const val TYPE_USER = "user"
        const val TYPE_PBX = "!user"
    }

    suspend fun fetchContacts(
        domain: String,
        token: String,
        userAgent: String,
        type: String,
        offset: Int,
        query: String = ""
    ): List<ContactResponse> {
        return try {
            val api = RetrofitClient.createContactsApi(domain)
            val contacts = api.getContacts(
                token     = token,
                userAgent = userAgent,
                tenant    = domain,
                type      = type,
                offset    = offset,
                limit     = LoquaceConfig.CONTACTS_PAGE_SIZE,
                timestamp = System.currentTimeMillis(),
                query     = query
            )
            contacts
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch contacts: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchContactPhoto(
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
            ).bytes()  // convert to ByteArray inside the module
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch photo at $photoPath: ${e.message}")
            null
        }
    }
}