package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presence")
data class PresenceEntity(
    @PrimaryKey val id: Int = 0,
    val message: String? = null,
    val status: String? = null,
    val name: String? = null,
    val account: String? = null,
    val photoUrl: String? = null
)