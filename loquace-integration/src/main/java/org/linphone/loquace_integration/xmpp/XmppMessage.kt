package org.linphone.loquace_integration.xmpp

data class XmppMessage(
    val id: String,
    val from: String,
    val to: String,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean
) {
    val formattedTime: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
}