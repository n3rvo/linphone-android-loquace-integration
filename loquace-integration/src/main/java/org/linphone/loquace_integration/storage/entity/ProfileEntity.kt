package org.linphone.loquace_integration.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val email: String,
    val organization: String,
    val organizationalUnit: String,
    val role: String,
    val avatarUrl: String,
    val phones: String             // JSON array string
)