package org.linphone.loquace_integration.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val prefs = try {
        EncryptedSharedPreferences.create(
            context,
            "loquace_session",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Keys are corrupted or missing — wipe and recreate
        context.deleteSharedPreferences("loquace_session")
        EncryptedSharedPreferences.create(
            context,
            "loquace_session",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun saveDbKey(key: String) = prefs.edit().putString(KEY_DB_KEY, key).apply()

    fun getDbKey(): String? = prefs.getString(KEY_DB_KEY, null)

    fun saveDomain(domain: String) = prefs.edit().putString(KEY_DOMAIN, domain).apply()

    fun getDomain(): String? = prefs.getString(KEY_DOMAIN, null)

    fun saveUserAgent(userAgent: String) {
        prefs.edit().putString("user_agent", userAgent).apply()
    }

    fun getUserAgent(): String {
        return prefs.getString("user_agent", "") ?: ""
    }

    companion object {
        private const val KEY_TOKEN  = "auth_token"
        private const val KEY_DB_KEY = "db_key"
        private const val KEY_DOMAIN = "domain"
    }
}