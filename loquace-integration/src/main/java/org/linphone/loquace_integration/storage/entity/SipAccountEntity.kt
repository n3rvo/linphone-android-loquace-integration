package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sip_account")
data class SipAccountEntity(
    @PrimaryKey val id: Int = 0,   // single row, always id = 0
    val username: String,
    val password: String,
    val domain: String,
    val proxy: String,
    val transport: String,
    val wsServers: String,         // JSON array string e.g. ["wss://..."]
    val codecs: String             // JSON array string e.g. ["opus", "PCMU"]
)