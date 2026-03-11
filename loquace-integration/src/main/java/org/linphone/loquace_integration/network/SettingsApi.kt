package org.linphone.loquace_integration.network

import retrofit2.http.GET
import retrofit2.http.Header

interface SettingsApi {
    @GET(LoquaceConfig.ENDPOINT_SETTINGS)
    suspend fun getSettings(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String
    ): SettingsResponse

    @GET(LoquaceConfig.ENDPOINT_SETTINGS_CALLS)
    suspend fun getCallsSettings(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String
    ): CallsResponse
}