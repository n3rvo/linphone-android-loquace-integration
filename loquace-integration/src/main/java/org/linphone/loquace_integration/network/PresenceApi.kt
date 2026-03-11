package org.linphone.loquace_integration.network

import retrofit2.http.GET
import retrofit2.http.Header

interface PresenceApi {
    @GET(LoquaceConfig.ENDPOINT_PRESENCE)
    suspend fun getPresence(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String
    ): PresenceResponse
}