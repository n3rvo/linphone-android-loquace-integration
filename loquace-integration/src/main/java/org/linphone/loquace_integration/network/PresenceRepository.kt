package org.linphone.loquace_integration.network

import android.util.Log
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.entity.PresenceEntity

class PresenceRepository(private val db: LoquaceDatabase) {

    suspend fun fetchAndStore(domain: String, token: String, userAgent: String) {
        Log.d(TAG, "Fetching presence for domain: $domain")
        val api = RetrofitClient.createPresenceApi(domain)
        val response = api.getPresence(
            token     = token,
            userAgent = userAgent,
            tenant    = domain
        )
        Log.d(TAG, "Presence received - status: ${response.status}, name: ${response.name}")
        db.presenceDao().save(
            PresenceEntity(
                message  = response.message,
                status   = response.status,
                name     = response.name,
                account  = response.account,
                photoUrl = response.photoUrl
            )
        )
        Log.d(TAG, "Presence stored successfully")
    }

    companion object {
        private const val TAG = "LoquacePresence"
    }
}