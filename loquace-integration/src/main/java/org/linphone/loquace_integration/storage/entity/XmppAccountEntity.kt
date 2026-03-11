package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xmpp_account")
data class XmppAccountEntity(
    @PrimaryKey val id: Int = 0,
    val enabled: Boolean,
    val username: String,
    val password: String,
    val domain: String,
    val serverAddress: String,
    val serverPort: Int
)