package org.linphone.loquace_integration.xmpp

sealed class XmppConnectionState {
    object Disconnected : XmppConnectionState()
    object Connecting : XmppConnectionState()
    object Connected : XmppConnectionState()
    data class Error(val message: String) : XmppConnectionState()
}