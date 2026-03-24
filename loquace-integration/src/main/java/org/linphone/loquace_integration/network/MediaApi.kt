package org.linphone.loquace_integration.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface MediaApi {
    @GET
    suspend fun downloadMedia(
        @Url url: String,
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_TENANT) tenant: String
    ): ResponseBody
}