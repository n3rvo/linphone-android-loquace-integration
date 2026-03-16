package org.linphone.loquace_integration.network

import android.util.Log
import org.linphone.loquace_integration.network.LoquaceConfig

class LoquaceCallHistoryRepository {

    companion object {
        private const val TAG = "LoquaceCallHistory"
        const val TAB_ALL = "all"
        const val TAB_MISSED = "missed"
    }

    suspend fun fetchCalls(
        domain: String,
        token: String,
        userAgent: String,
        offset: Int,
        missedOnly: Boolean = false
    ): List<CallHistoryResponse> {
        return try {
            val api = RetrofitClient.createCallHistoryApi(domain)
            val calls = api.getCalls(
                token     = token,
                userAgent = userAgent,
                tenant    = domain,
                offset    = offset,
                limit     = LoquaceConfig.CONTACTS_PAGE_SIZE,
                timestamp = System.currentTimeMillis(),
                seen      = if (missedOnly) false else null
            )
            Log.d(TAG, "Fetched ${calls.size} calls at offset $offset, missedOnly=$missedOnly")
            calls
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch calls: ${e.message}")
            emptyList()
        }
    }
}