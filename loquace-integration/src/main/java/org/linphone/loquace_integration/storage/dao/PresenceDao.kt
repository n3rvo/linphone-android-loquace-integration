package org.linphone.loquace_integration.storage.dao

import androidx.room.*
import org.linphone.loquace_integration.storage.entity.PresenceEntity

@Dao
interface PresenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: PresenceEntity)

    @Query("SELECT * FROM presence WHERE id = 0")
    suspend fun get(): PresenceEntity?

    @Query("DELETE FROM presence")
    suspend fun clear()
}