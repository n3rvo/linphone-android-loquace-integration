package org.linphone.loquace_integration.storage.dao

import androidx.room.*
import org.linphone.loquace_integration.storage.entity.SipAccountEntity

@Dao
interface SipAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: SipAccountEntity)

    @Query("SELECT * FROM sip_account WHERE id = 0")
    suspend fun get(): SipAccountEntity?

    @Query("DELETE FROM sip_account")
    suspend fun clear()
}

/*
LoquaceDatabase  →  the whole database
SipAccountEntity →  the table structure (columns)
SipAccountDao    →  the questions you can ask / actions you can do on that table
*/