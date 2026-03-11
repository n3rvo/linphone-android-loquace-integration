package org.linphone.loquace_integration.storage.dao

import androidx.room.*
import org.linphone.loquace_integration.storage.entity.XmppAccountEntity

@Dao
interface XmppAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: XmppAccountEntity)

    @Query("SELECT * FROM xmpp_account WHERE id = 0")
    suspend fun get(): XmppAccountEntity?

    @Query("DELETE FROM xmpp_account")
    suspend fun clear()
}