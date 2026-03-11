package org.linphone.loquace_integration.network

data class PresenceResponse(
    val message: String,
    val status: String,
    val name: String,
    val account: String,
    val photoUrl: String
)