package org.linphone.loquace_integration.network

import android.util.Log

object LoquaceMediaDownloader {

    private const val TAG = "LoquaceMediaDownloader"

    suspend fun downloadBytes(
        url: String,
        token: String,
        domain: String
    ): ByteArray? {
        return try {
            val api = RetrofitClient.createMediaApi(domain)
            val response = api.downloadMedia(
                url     = url,
                token   = token,
                tenant  = domain
            )
            response.bytes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download media: ${e.message}")
            null
        }
    }
}