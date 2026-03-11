package org.linphone.loquace_integration.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.linphone.loquace_integration.storage.dao.*
import org.linphone.loquace_integration.storage.entity.*

@Database(
    entities = [
        SipAccountEntity::class,
        ProfileEntity::class,
        XmppAccountEntity::class,
        CallsEntity::class,
        PresenceEntity::class
    ],
    version = 2,         // bump version since we added a table
    exportSchema = false
)
abstract class LoquaceDatabase : RoomDatabase() {
    abstract fun sipAccountDao(): SipAccountDao
    abstract fun profileDao(): ProfileDao
    abstract fun xmppAccountDao(): XmppAccountDao
    abstract fun callsDao(): CallsDao
    abstract fun presenceDao(): PresenceDao

    companion object {
        @Volatile private var INSTANCE: LoquaceDatabase? = null

        fun getInstance(context: Context): LoquaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = SQLiteDatabase.getBytes(getDatabaseKey(context).toCharArray())
                val factory = SupportFactory(passphrase)

                Room.databaseBuilder(
                    context.applicationContext,
                    LoquaceDatabase::class.java,
                    "loquace_db"
                )
                    .openHelperFactory(factory) // Comment this when we want to see the db data trough App Inspection tool
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