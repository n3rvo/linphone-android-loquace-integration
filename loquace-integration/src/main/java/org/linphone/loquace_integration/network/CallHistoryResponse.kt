package org.linphone.loquace_integration.network

data class CallHistoryResponse(
    val id: String,
    val terminationCause: String?,
    val duration: Int,
    val dateTime: Long,
    val direction: String,
    val account: String?,
    val seen: Boolean,
    val date: String?,
    val type: String?,
    val missed: Boolean,
    val contact: CallHistoryContact?
)

data class CallHistoryContact(
    val id: String?,
    val pictureUrl: String?,
    val name: String?,
    val number: String?,
    val type: String?
)