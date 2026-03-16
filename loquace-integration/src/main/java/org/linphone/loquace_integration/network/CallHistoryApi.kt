package org.linphone.loquace_integration.network

import org.linphone.loquace_integration.network.LoquaceConfig
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CallHistoryApi {
    @GET(LoquaceConfig.ENDPOINT_CALLS)
    suspend fun getCalls(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Query("offset")                         offset: Int,
        @Query("limit")                          limit: Int,
        @Query("_")                              timestamp: Long,
        @Query("seen")                           seen: Boolean? = null
    ): List<CallHistoryResponse>
}