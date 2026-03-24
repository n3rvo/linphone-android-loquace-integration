package org.linphone.loquace_integration.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private fun buildRetrofit(domain: String): Retrofit {
        val baseUrl = "https://$domain:${LoquaceConfig.PORT}/${LoquaceConfig.API_PREFIX}/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthApi(domain: String): AuthApi =
        buildRetrofit(domain).create(AuthApi::class.java)

    fun createSettingsApi(domain: String): SettingsApi =
        buildRetrofit(domain).create(SettingsApi::class.java)

    fun createPresenceApi(domain: String): PresenceApi =
        buildRetrofit(domain).create(PresenceApi::class.java)

    fun createContactsApi(domain: String): ContactsApi =
        buildRetrofit(domain).create(ContactsApi::class.java)

    fun createCallHistoryApi(domain: String): CallHistoryApi =
        buildRetrofit(domain).create(CallHistoryApi::class.java)

    fun createMediaApi(domain: String): MediaApi =
        buildRetrofit(domain).create(MediaApi::class.java)
}