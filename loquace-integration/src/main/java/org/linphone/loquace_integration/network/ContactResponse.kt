package org.linphone.loquace_integration.network

data class ContactsPageResponse(
    val contacts: List<ContactResponse>
)

data class ContactResponse(
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val fullName: String?,
    val type: String,
    val phones: List<ContactPhone>?,
    val presence: ContactPresence?,
    val email: String?,
    val organization: String?,
    val account: String?,
    val pictureUrl: String? = null,
    val chats: List<ContactChat>? = null
)

data class ContactPhone(
    val number: String,
    val type: String,
    val status: String
)

data class ContactPresence(
    val status: String
)

data class ContactChat(
    val account: String?,
    val type: String?
)