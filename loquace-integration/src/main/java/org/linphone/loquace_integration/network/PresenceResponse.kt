package org.linphone.loquace_integration.network

data class PresenceResponse(
    val message: String? = null,
    val status: String? = null,
    val name: String? = null,
    val account: String? = null,
    val photoUrl: String? = null
)