package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallsEntity(
    @PrimaryKey val id: Int = 0,
    val inboundDevices: String     // JSON array string
)