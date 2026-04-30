package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xmpp_conversations")
data class XmppConversationEntity(
    @PrimaryKey
    val peerJid: String,
    val displayName: String?,
    val lastMessage: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val isGroup: Boolean,
    val pictureUrl: String?
)