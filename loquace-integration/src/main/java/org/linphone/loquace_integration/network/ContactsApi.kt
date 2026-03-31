package org.linphone.loquace_integration.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface ContactsApi {

    @GET(LoquaceConfig.ENDPOINT_CONTACTS)
    suspend fun getContacts(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Query("type")                           type: String? = null,
        @Query("offset")                         offset: Int,
        @Query("limit")                          limit: Int,
        @Query("_")                              timestamp: Long,
        @Query("term")                           query: String = "",
        @Query("chats")                          chatsEnabled: Boolean? = null
    ): List<ContactResponse>

    @GET
    suspend fun getContactPhoto(
        @Url url: String,
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String
    ): ResponseBody

    @GET(LoquaceConfig.ENDPOINT_CONTACTS)
    suspend fun getContactByJid(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Query("chats")                          jid: String,
        @Query("_")                              timestamp: Long = System.currentTimeMillis()
    ): List<ContactResponse>
}