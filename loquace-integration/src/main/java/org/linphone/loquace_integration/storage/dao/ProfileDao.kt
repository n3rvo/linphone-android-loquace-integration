package org.linphone.loquace_integration.storage.dao

import androidx.room.*
import org.linphone.loquace_integration.storage.entity.ProfileEntity

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = 0")
    suspend fun get(): ProfileEntity?

    @Query("DELETE FROM profile")
    suspend fun clear()
}