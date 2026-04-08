package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val organization: String? = null,
    val organizationalUnit: String? = null,
    val role: String? = null,
    val avatarUrl: String? = null,
    val phones: String? = null
)