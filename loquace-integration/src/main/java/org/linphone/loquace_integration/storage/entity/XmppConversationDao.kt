package org.linphone.loquace_integration.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.linphone.loquace_integration.storage.entity.XmppConversationEntity

@Dao
interface XmppConversationDao {
    @Query("SELECT * FROM xmpp_conversations ORDER BY lastTimestamp DESC")
    suspend fun getAll(): List<XmppConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: XmppConversationEntity)

    @Query("DELETE FROM xmpp_conversations WHERE peerJid = :peerJid")
    suspend fun deleteByPeerJid(peerJid: String)

    @Query("DELETE FROM xmpp_conversations")
    suspend fun deleteAll()
}