package org.linphone.loquace_integration.storage.dao

import androidx.room.*
import org.linphone.loquace_integration.storage.entity.CallsEntity

@Dao
interface CallsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: CallsEntity)

    @Query("SELECT * FROM calls WHERE id = 0")
    suspend fun get(): CallsEntity?

    @Query("DELETE FROM calls")
    suspend fun clear()
}