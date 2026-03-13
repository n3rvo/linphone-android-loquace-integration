package org.linphone.loquace_integration.network

object LoquaceConfig {
    const val PORT       = "443"
    const val API_PREFIX = "api/v2"

    // Endpoints
    const val ENDPOINT_AUTH          = "auth/tokens"
    const val ENDPOINT_SETTINGS      = "settings"
    const val ENDPOINT_SETTINGS_CALLS = "settings/calls"
    const val ENDPOINT_PRESENCE      = "status/presence"
    const val ENDPOINT_CONTACTS = "contacts"

    // Contacts page size
    const val CONTACTS_PAGE_SIZE = 30

    // Headers
    const val HEADER_AUTH_TOKEN = "x-auth-token"
    const val HEADER_USER_AGENT = "user-agent"
    const val HEADER_TENANT     = "x-loquace-tenant"

}