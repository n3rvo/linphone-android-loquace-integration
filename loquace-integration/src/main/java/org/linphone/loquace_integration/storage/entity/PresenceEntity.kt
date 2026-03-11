package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presence")
data class PresenceEntity(
    @PrimaryKey val id: Int = 0,
    val message: String,
    val status: String,
    val name: String,
    val account: String,
    val photoUrl: String
)