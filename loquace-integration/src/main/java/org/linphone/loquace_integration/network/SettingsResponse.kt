package org.linphone.loquace_integration.network

data class SettingsResponse(
    val id: String,
    val sipAccount: SipAccount,
    val xmppAccount: XmppAccount,
    val fusionpbx: Fusionpbx,
    val xmpp: Xmpp,
    val locale: Locale,
    val web: Web,
    val calls: Calls,
    val profile: Profile
)

data class SipAccount(
    val username: String,
    val audio: Audio,
    val transport: String,
    val proxy: String,
    val domain: String,
    val wsServers: List<String>,
    val password: String
)

data class Audio(
    val codecs: List<String>
)

data class XmppAccount(
    val enabled: Boolean,
    val username: String,
    val serverAddress: String,
    val serverPort: Int,
    val domain: String,
    val password: String
)

data class Fusionpbx(
    val api: FusionpbxApi
)

data class FusionpbxApi(
    val url: String
)

data class Xmpp(
    val port: Int,
    val enabled: Boolean,
    val ejabberd: Ejabberd,
    val domain: String,
    val server: String
)

data class Ejabberd(
    val api: EjabberdApi
)

data class EjabberdApi(
    val url: String
)

data class Locale(
    val language: String
)

data class Web(
    val home: String,
    val rtc: Rtc,
    val ws: Ws
)

data class Rtc(
    val enabled: Boolean,
    val ws: List<String>
)

data class Ws(
    val heartbeat: Int,
    val path: String
)

data class Calls(
    val inbound: Inbound
)

data class Inbound(
    val devices: List<Device>
)

data class Device(
    val enabled: Boolean,
    val type: String
)

data class Profile(
    val lastName: String,
    val organization: String,
    val email: String,
    val avatarUrl: String,
    val role: String,
    val firstName: String,
    val organizationalUnit: String,
    val phones: List<Phone>
)

data class Phone(
    val number: String,
    val type: String
)