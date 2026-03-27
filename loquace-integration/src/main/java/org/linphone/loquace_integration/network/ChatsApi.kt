package org.linphone.loquace_integration.network

import retrofit2.http.*

interface ChatsApi {

    @GET("chats")
    suspend fun getGroups(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Query("type")                           type: String = "group"
    ): List<GroupResponse>

    @POST("chats")
    suspend fun createGroup(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Body                                    request: CreateGroupRequest
    ): GroupResponse

    @POST("chats/{groupJid}/participants/{userJid}")
    suspend fun inviteParticipant(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Path("groupJid")                        groupJid: String,
        @Path("userJid")                         userJid: String,
        @Body                                    request: InviteParticipantRequest
    ): GroupParticipant

    @GET("chats/{groupJid}")
    suspend fun getGroupDetails(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Path("groupJid")                        groupJid: String
    ): GroupResponse

    @DELETE("chats/{groupJid}/participants/{userJid}")
    suspend fun removeParticipant(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Path("groupJid")                        groupJid: String,
        @Path("userJid")                         userJid: String
    ): GroupResponse

    @DELETE("chats/{groupJid}")
    suspend fun deleteGroup(
        @Header(LoquaceConfig.HEADER_AUTH_TOKEN) token: String,
        @Header(LoquaceConfig.HEADER_USER_AGENT) userAgent: String,
        @Header(LoquaceConfig.HEADER_TENANT)     tenant: String,
        @Path("groupJid")                        groupJid: String
    ): GroupResponse
}