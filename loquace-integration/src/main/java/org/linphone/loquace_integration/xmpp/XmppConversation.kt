package org.linphone.loquace_integration.xmpp

data class XmppConversation(
    val peerJid: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val displayName: String? = null,
    val pictureUrl: String? = null  // Add this
)