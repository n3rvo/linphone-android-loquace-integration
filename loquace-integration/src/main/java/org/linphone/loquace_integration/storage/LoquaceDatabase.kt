package org.linphone.loquace_integration.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.linphone.loquace_integration.storage.dao.*
import org.linphone.loquace_integration.storage.entity.*

@Database(
    entities = [
        SipAccountEntity::class,
        ProfileEntity::class,
        XmppAccountEntity::class,
        CallsEntity::class,
        PresenceEntity::class,
        XmppConversationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LoquaceDatabase : RoomDatabase() {
    abstract fun sipAccountDao(): SipAccountDao
    abstract fun profileDao(): ProfileDao
    abstract fun xmppAccountDao(): XmppAccountDao
    abstract fun callsDao(): CallsDao
    abstract fun presenceDao(): PresenceDao
    abstract fun xmppConversationDao(): XmppConversationDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                CREATE TABLE IF NOT EXISTS xmpp_conversations (
                    peerJid TEXT NOT NULL PRIMARY KEY,
                    displayName TEXT,
                    lastMessage TEXT,
                    lastTimestamp INTEGER NOT NULL,
                    unreadCount INTEGER NOT NULL,
                    isGroup INTEGER NOT NULL,
                    pictureUrl TEXT
                )
            """)
            }
        }

        @Volatile private var INSTANCE: LoquaceDatabase? = null

        fun getInstance(context: Context): LoquaceDatabase {
            return INSTANCE ?: synchronized(this) {
                System.loadLibrary("sqlcipher")

                val passphrase = getDatabaseKey(context).toByteArray(Charsets.UTF_8)
                val factory = SupportOpenHelperFactory(passphrase)

                Room.databaseBuilder(
                    context.applicationContext,
                    LoquaceDatabase::class.java,
                    "loquace_db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        // The encryption key is stored in EncryptedSharedPreferences
        // so it's never hardcoded
        private fun getDatabaseKey(context: Context): String {
            val prefs = SessionManager(context)
            return prefs.getDbKey() ?: run {
                val newKey = java.util.UUID.randomUUID().toString()
                prefs.saveDbKey(newKey)
                newKey
            }
        }
    }
}