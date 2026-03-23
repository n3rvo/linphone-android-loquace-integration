package org.linphone.loquace_integration.xmpp

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.impl.JidCreate
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.entity.XmppAccountEntity
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

object LoquaceXmppManager {

    private const val TAG = "LoquaceXmppManager"

    private var isConnecting = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var connection: XMPPTCPConnection? = null

    private var chatManager: ChatManager? = null

    // Connection state
    private val _connectionState = MutableStateFlow<XmppConnectionState>(XmppConnectionState.Disconnected)
    val connectionState: StateFlow<XmppConnectionState> = _connectionState

    // Conversations cache
    private val _conversations = MutableStateFlow<List<XmppConversation>>(emptyList())
    val conversations: StateFlow<List<XmppConversation>> = _conversations

    // Messages cache
    private val _messages = MutableStateFlow<Map<String, List<XmppMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<XmppMessage>>> = _messages

    fun connect(account: XmppAccountEntity, userAgent: String) {
        if (isConnected() || isConnecting) {
            Log.d(TAG, "Already connected or connecting, skipping")
            return
        }
        isConnecting = true
        scope.launch {
            try {
                Log.d(TAG, "Connecting to XMPP server ${account.serverAddress}:${account.serverPort}")
                _connectionState.value = XmppConnectionState.Connecting

                val config = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword(account.username, account.password)
                    .setXmppDomain(JidCreate.domainBareFrom(account.domain))
                    .setHost(account.serverAddress)
                    .setPort(account.serverPort)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                    .setResource(userAgent)
                    .setSendPresence(true)
                    .build()

                val conn = XMPPTCPConnection(config)
                Roster.getInstanceFor(conn).isRosterLoadedAtLogin = false  // Add this
                connection = conn

                ReconnectionManager.getInstanceFor(conn).apply {
                    enableAutomaticReconnection()
                    setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY)
                }

                Log.d(TAG, "Connecting as ${account.username}@${account.domain} to ${account.serverAddress}:${account.serverPort}")
                conn.connect()
                conn.login()

                chatManager = ChatManager.getInstanceFor(conn)
                chatManager?.addIncomingListener(IncomingChatMessageListener { from, message, chat ->
                    scope.launch {
                        val xmppMessage = XmppMessage(
                            id          = message.stanzaId ?: System.currentTimeMillis().toString(),
                            from        = from.asEntityBareJidString(),
                            to          = conn.user.asEntityBareJidString(),
                            body        = message.body ?: "",
                            timestamp   = System.currentTimeMillis(),
                            isOutgoing  = false
                        )
                        Log.d(TAG, "Received message from ${xmppMessage.from}: ${xmppMessage.body}")
                        updateConversationWithMessage(xmppMessage)
                    }
                })

                Log.d(TAG, "Connected and logged in as ${conn.user}")
                _connectionState.value = XmppConnectionState.Connected

                loadConversations()

                isConnecting = false

            } catch (e: XMPPException) {
                Log.e(TAG, "XMPP error during connection: ${e.message}")
                isConnecting = false
                _connectionState.value = XmppConnectionState.Error(e.message ?: "XMPP error")
            } catch (e: SmackException) {
                Log.e(TAG, "Smack error during connection: ${e.message}")
                isConnecting = false
                _connectionState.value = XmppConnectionState.Error(e.message ?: "Smack error")
            } catch (e: Exception) {
                Log.e(TAG, "Error during connection: ${e.message}")
                isConnecting = false
                _connectionState.value = XmppConnectionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                connection?.disconnect()
                connection = null
                chatManager = null
                _connectionState.value = XmppConnectionState.Disconnected
                Log.d(TAG, "Disconnected from XMPP server")
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect: ${e.message}")
            }
        }
    }

    fun sendMessage(toJid: String, body: String): XmppMessage? {
        return try {
            val conn = connection ?: throw Exception("Not connected")
            val jid: EntityBareJid = JidCreate.entityBareFrom(toJid)
            val chat: Chat = ChatManager.getInstanceFor(conn).chatWith(jid)
            val messageId = System.currentTimeMillis().toString()
            chat.send(body)

            val message = XmppMessage(
                id         = messageId,
                from       = conn.user.asEntityBareJidString(),
                to         = toJid,
                body       = body,
                timestamp  = System.currentTimeMillis(),
                isOutgoing = true
            )
            scope.launch {
                updateConversationWithMessage(message)
            }
            Log.d(TAG, "Sent message to $toJid: $body")
            message
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            null
        }
    }

    fun isConnected(): Boolean = connection?.isConnected == true && connection?.isAuthenticated == true

    fun getMyJid(): String = connection?.user?.asEntityBareJidString() ?: ""

    private fun loadConversations() {
        // Initial empty list — conversations are built up as messages arrive
        // In a future phase we'll load from local DB cache
        _conversations.value = emptyList()
    }

    private suspend fun updateConversationWithMessage(message: XmppMessage) {
        storeMessage(message)
        val peerJid = if (message.isOutgoing) message.to else message.from
        val currentList = _conversations.value.toMutableList()
        val existing = currentList.find { it.peerJid == peerJid }

        if (existing != null) {
            val updated = existing.copy(
                lastMessage   = message.body,
                lastTimestamp = message.timestamp,
                unreadCount   = if (message.isOutgoing) existing.unreadCount
                else existing.unreadCount + 1
            )
            currentList[currentList.indexOf(existing)] = updated
        } else {
            currentList.add(0, XmppConversation(
                peerJid       = peerJid,
                lastMessage   = message.body,
                lastTimestamp = message.timestamp,
                unreadCount   = if (message.isOutgoing) 0 else 1
            ))
        }

        currentList.sortByDescending { it.lastTimestamp }
        _conversations.value = currentList
    }

    fun getMessagesForConversation(peerJid: String): List<XmppMessage> {
        return _messages.value[peerJid] ?: emptyList()
    }

    private suspend fun storeMessage(message: XmppMessage) {
        val peerJid = if (message.isOutgoing) message.to else message.from
        val current = _messages.value.toMutableMap()
        val conversationMessages = (current[peerJid] ?: emptyList()).toMutableList()
        conversationMessages.add(message)
        current[peerJid] = conversationMessages
        _messages.value = current
    }
}