package org.linphone.loquace_integration.network

data class GroupResponse(
    val id: String,
    val createdAt: Long,
    val name: String,
    val jid: String,
    val type: String,
    val participants: List<GroupParticipant> = emptyList()
)

data class GroupParticipant(
    val id: String?,
    val role: String,
    val account: String,
    val fullName: String?
)

data class CreateGroupRequest(
    val type: String = "group",
    val name: String
)

data class InviteParticipantRequest(
    val role: String = "member"
)