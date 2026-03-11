package org.linphone.loquace_integration.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST(LoquaceConfig.ENDPOINT_AUTH)
    suspend fun login(
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header("x-loquace-mobile-push-token")   mobilePushToken: String,
        @Header("x-loquace-push-token")          pushToken: String,
        @Body request: LoginRequest
    ): AuthResponse
}