package org.linphone.loquace_integration.xmpp

data class XmppMessage(
    val id: String,
    val from: String,
    val to: String,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean
)