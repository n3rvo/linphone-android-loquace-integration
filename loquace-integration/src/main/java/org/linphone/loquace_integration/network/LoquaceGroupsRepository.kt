package org.linphone.loquace_integration.network

import android.util.Log

class LoquaceGroupsRepository {

    companion object {
        private const val TAG = "LoquaceGroups"
    }

    suspend fun getGroups(
        domain: String,
        token: String,
        userAgent: String
    ): List<GroupResponse> {
        return try {
            val api = RetrofitClient.createChatsApi(domain)
            api.getGroups(token, userAgent, domain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch groups: ${e.message}")
            emptyList()
        }
    }

    suspend fun createGroup(
        domain: String,
        token: String,
        userAgent: String,
        name: String
    ): GroupResponse? {
        return try {
            val api = RetrofitClient.createChatsApi(domain)
            api.createGroup(token, userAgent, domain, CreateGroupRequest(name = name))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create group: ${e.message}")
            null
        }
    }

    suspend fun inviteParticipant(
        domain: String,
        token: String,
        userAgent: String,
        groupJid: String,
        userJid: String
    ): GroupParticipant? {
        return try {
            val api = RetrofitClient.createChatsApi(domain)
            api.inviteParticipant(token, userAgent, domain, groupJid, userJid, InviteParticipantRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invite participant: ${e.message}")
            null
        }
    }

    suspend fun removeParticipant(
        domain: String,
        token: String,
        userAgent: String,
        groupJid: String,
        userJid: String
    ): Boolean {
        return try {
            val api = RetrofitClient.createChatsApi(domain)
            api.removeParticipant(token, userAgent, domain, groupJid, userJid)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove participant: ${e.message}")
            false
        }
    }

    suspend fun deleteGroup(
        domain: String,
        token: String,
        userAgent: String,
        groupJid: String
    ): Boolean {
        return try {
            val api = RetrofitClient.createChatsApi(domain)
            api.deleteGroup(token, userAgent, domain, groupJid)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete group: ${e.message}")
            false
        }
    }

    suspend fun fetchChatEnabledContacts(
        domain: String,
        token: String,
        userAgent: String,
        offset: Int,
        query: String = ""
    ): List<ContactResponse> {
        return try {
            val api = RetrofitClient.createContactsApi(domain)
            api.getContacts(
                token        = token,
                userAgent    = userAgent,
                tenant       = domain,
                type         = null,
                offset       = offset,
                limit        = LoquaceConfig.CONTACTS_PAGE_SIZE,
                timestamp    = System.currentTimeMillis(),
                query        = query,
                chatsEnabled = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch chat-enabled contacts: ${e.message}")
            emptyList()
        }
    }
}