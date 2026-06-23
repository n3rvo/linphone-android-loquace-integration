package org.linphone.loquace_integration.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface CallsApi {
    @POST(LoquaceConfig.ENDPOINT_PLACE_CALL)
    suspend fun placeCall(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT) tenant: String,
        @Body request: CallRequest
    ): CallResponse
}

data class CallRequest(
    val contact: CallContactRequest
)

data class CallContactRequest(
    val id: String? = null,
    val name: String? = null,
    val number: String
)

data class CallResponse(
    val failed: Boolean,
    val account: String? = null,
    val type: String? = null,
    val contact: CallContactResponse
)

data class CallContactResponse(
    val id: String? = null,
    val name: String? = null,
    val number: String,
    val type: String? = null
)