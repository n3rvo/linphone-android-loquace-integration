package org.linphone.loquace_integration.xmpp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.mam.MamManager
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import org.linphone.loquace_integration.storage.LoquaceDatabase
import org.linphone.loquace_integration.storage.entity.XmppAccountEntity
import org.linphone.loquace_integration.storage.entity.XmppConversationEntity
import java.io.File

object LoquaceXmppManager {

    private const val TAG = "LoquaceXmppManager"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private var isConnecting = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connection: XMPPTCPConnection? = null
    private var chatManager: ChatManager? = null
    private val sentMessageIds = mutableSetOf<String>()

    private val _connectionState = MutableStateFlow<XmppConnectionState>(XmppConnectionState.Disconnected)
    val connectionState: StateFlow<XmppConnectionState> = _connectionState

    private val _conversations = MutableStateFlow<List<XmppConversation>>(emptyList())
    val conversations: StateFlow<List<XmppConversation>> = _conversations

    private val _messages = MutableStateFlow<Map<String, List<XmppMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<XmppMessage>>> = _messages

    private val joinedRooms = mutableMapOf<String, MultiUserChat>()
    private val roomsWithListeners = mutableSetOf<String>()

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
                Roster.getInstanceFor(conn).isRosterLoadedAtLogin = false
                connection = conn

                Log.d(TAG, "Connecting as ${account.username}@${account.domain} to ${account.serverAddress}:${account.serverPort}")
                conn.connect()
                conn.login()

                chatManager = ChatManager.getInstanceFor(conn)
                chatManager?.addIncomingListener(IncomingChatMessageListener { from, message, chat ->
                    scope.launch {
                        Log.d(TAG, "Incoming raw stanza: ${message.toXML()}")
                        val body = message.body ?: ""
                        Log.d(TAG, "Incoming message from ${from.asEntityBareJidString()}, body: $body")
                        Log.d(TAG, "My JID: ${conn.user.asEntityBareJidString()}")
                        Log.d(TAG, "sentMessageIds contains body: ${sentMessageIds.contains(body)}")

                        val retractExtension = message.extensions.find {
                            it.namespace == "urn:xmpp:message-retract:1" && it.elementName == "retract"
                        }
                        if (retractExtension != null) {
                            val retractedId = retractExtension.toXML()
                                .toString()
                                .substringAfter("id='")
                                .substringBefore("'")
                            val peerJid = from.asEntityBareJidIfPossible()?.toString() ?: return@launch
                            Log.d(TAG, "Received retraction for message $retractedId from $peerJid")
                            retractLocalMessage(peerJid, retractedId)
                            return@launch
                        }

                        val correctionExtension = message.extensions.find {
                            it.namespace == "urn:xmpp:message-correct:0" && it.elementName == "replace"
                        }
                        if (correctionExtension != null) {
                            val correctedId = correctionExtension.toXML()
                                .toString()
                                .substringAfter("id='")
                                .substringBefore("'")
                            val peerJid = from.asEntityBareJidIfPossible()?.toString() ?: return@launch
                            Log.d(TAG, "Received correction for message $correctedId from $peerJid")
                            updateMessageBody(peerJid, correctedId, body)
                            return@launch
                        }

                        if (sentMessageIds.contains(body)) {
                            Log.d(TAG, "Ignoring carbon copy of our own message")
                            sentMessageIds.remove(body)
                            return@launch
                        }

                        val attachmentType = detectAttachmentType(body)
                        Log.d(TAG, "Incoming message id=${message.stanzaId}, attachmentType=$attachmentType, body=$body")
                        val attachmentName = if (attachmentType != AttachmentType.NONE) {
                            body.substringAfterLast("/").substringBefore("?")
                        } else null

                        val xmppMessage = XmppMessage(
                            id             = message.stanzaId ?: System.currentTimeMillis().toString(),
                            from           = from.asEntityBareJidString(),
                            to             = conn.user.asEntityBareJidString(),
                            body           = if (attachmentType != AttachmentType.NONE) "" else body,
                            timestamp      = System.currentTimeMillis(),
                            isOutgoing     = false,
                            attachmentUrl  = if (attachmentType != AttachmentType.NONE) body else null,
                            attachmentType = attachmentType,
                            attachmentName = attachmentName
                        )
                        Log.d(TAG, "Received message from ${xmppMessage.from}")
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
                isConnecting = false
                _connectionState.value = XmppConnectionState.Disconnected
                Log.d(TAG, "Disconnected from XMPP server")
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect: ${e.message}")
            }
        }
    }

    fun logout() {
        scope.launch {
            try {
                connection?.disconnect()
                connection = null
                chatManager = null
                isConnecting = false
                joinedRooms.clear()
                roomsWithListeners.clear()
                _conversations.value = emptyList()
                _messages.value = emptyMap()
                _connectionState.value = XmppConnectionState.Disconnected
                Log.d(TAG, "XMPP logged out and state cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}")
            }
        }
    }

    fun sendMessage(toJid: String, messageBody: String): XmppMessage? {
        return try {
            val conn = connection ?: throw Exception("Not connected")
            val messageId = System.currentTimeMillis().toString()

            val muc = joinedRooms[toJid]
            if (muc != null) {
                sentMessageIds.add(messageBody)
                muc.sendMessage(messageBody)
                Log.d(TAG, "Sent MUC message to $toJid: $messageBody")
            } else {
                val sentMessage = org.jivesoftware.smack.packet.Message(
                    JidCreate.entityBareFrom(toJid),
                    org.jivesoftware.smack.packet.Message.Type.chat
                ).apply {
                    body = messageBody
                }
                sentMessageIds.add(messageBody)
                conn.sendStanza(sentMessage)
                Log.d(TAG, "Sent message stanzaId: ${sentMessage.stanzaId}")
                Log.d(TAG, "Sent message to $toJid: $messageBody")

                val message = XmppMessage(
                    id         = sentMessage.stanzaId ?: messageId,
                    from       = conn.user.asEntityBareJidString(),
                    to         = toJid,
                    body       = messageBody,
                    timestamp  = System.currentTimeMillis(),
                    isOutgoing = true
                )
                scope.launch { updateConversationWithMessage(message) }
                return message
            }

            val message = XmppMessage(
                id         = messageId,
                from       = conn.user.asEntityBareJidString(),
                to         = toJid,
                body       = messageBody,
                timestamp  = System.currentTimeMillis(),
                isOutgoing = true
            )
            scope.launch { updateConversationWithMessage(message) }
            message
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            null
        }
    }

    fun sendMessageWithAttachment(
        toJid: String,
        attachmentUrl: String,
        attachmentType: AttachmentType,
        attachmentName: String,
        body: String = ""
    ): XmppMessage? {
        return try {
            val conn = connection ?: throw Exception("Not connected")
            val muc = joinedRooms[toJid]
            if (muc != null) {
                muc.sendMessage(attachmentUrl)
            } else {
                val jid: EntityBareJid = JidCreate.entityBareFrom(toJid)
                val chat: Chat = ChatManager.getInstanceFor(conn).chatWith(jid)
                chat.send(attachmentUrl)
            }

            val message = XmppMessage(
                id             = System.currentTimeMillis().toString(),
                from           = conn.user.asEntityBareJidString(),
                to             = toJid,
                body           = body,
                timestamp      = System.currentTimeMillis(),
                isOutgoing     = true,
                attachmentUrl  = attachmentUrl,
                attachmentType = attachmentType,
                attachmentName = attachmentName
            )
            scope.launch { updateConversationWithMessage(message) }
            Log.d(TAG, "Sent attachment to $toJid: $attachmentUrl")
            message
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send attachment: ${e.message}")
            null
        }
    }

    suspend fun uploadAndSendFile(
        toJid: String,
        file: File,
        attachmentName: String,
        attachmentType: AttachmentType,
        localPath: String? = null
    ): XmppMessage? {
        val conn = connection as? XMPPTCPConnection ?: run {
            Log.e(TAG, "Not connected or wrong connection type")
            return null
        }

        val messageId = System.currentTimeMillis().toString()
        val pendingMessage = XmppMessage(
            id             = messageId,
            from           = conn.user.asEntityBareJidString(),
            to             = toJid,
            body           = "",
            timestamp      = System.currentTimeMillis(),
            isOutgoing     = true,
            attachmentType = attachmentType,
            attachmentName = attachmentName,
            localPath      = localPath,
            isUploading    = true
        )
        addPendingMessage(pendingMessage)

        val uploadedUrl = XmppHttpUploadManager.uploadFile(conn, file) ?: run {
            Log.e(TAG, "Failed to upload file ${file.name}")
            return null
        }

        val muc = joinedRooms[toJid]
        if (muc != null) {
            val mucMessage = muc.createMessage().apply { body = uploadedUrl }
            muc.sendMessage(mucMessage)
            Log.d(TAG, "Sent MUC attachment stanzaId: ${mucMessage.stanzaId}")
            val finalMessage = pendingMessage.copy(
                id            = mucMessage.stanzaId ?: messageId,
                attachmentUrl = uploadedUrl,
                isUploading   = false
            )
            updateMessage(messageId, toJid, finalMessage)
            return finalMessage
        } else {
            val sentMessage = org.jivesoftware.smack.packet.Message(
                JidCreate.entityBareFrom(toJid),
                org.jivesoftware.smack.packet.Message.Type.chat
            ).apply { body = uploadedUrl }
            conn.sendStanza(sentMessage)
            Log.d(TAG, "Sent attachment stanzaId: ${sentMessage.stanzaId}")
            val finalMessage = pendingMessage.copy(
                id            = sentMessage.stanzaId ?: messageId,
                attachmentUrl = uploadedUrl,
                isUploading   = false
            )
            updateMessage(messageId, toJid, finalMessage)
            return finalMessage
        }
    }

    fun isConnected(): Boolean = connection?.isConnected == true && connection?.isAuthenticated == true

    fun getMyJid(): String = connection?.user?.asEntityBareJidString() ?: ""

    fun getConnection(): XMPPTCPConnection? = connection as? XMPPTCPConnection

    private fun loadConversations() {
        scope.launch {
            try {
                val db = LoquaceDatabase.getInstance(appContext)
                val entities = db.xmppConversationDao().getAll()
                _conversations.value = entities.map { entity ->
                    XmppConversation(
                        peerJid       = entity.peerJid,
                        lastMessage   = entity.lastMessage,
                        lastTimestamp = entity.lastTimestamp,
                        unreadCount   = entity.unreadCount,
                        isGroup       = entity.isGroup,
                        displayName   = entity.displayName
                    )
                }
                Log.d(TAG, "Loaded ${entities.size} conversations from DB")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversations from DB: ${e.message}")
                _conversations.value = emptyList()
            }
        }
    }

    private suspend fun updateConversationWithMessage(message: XmppMessage) {
        storeMessage(message)
        val peerJid = if (message.isOutgoing) message.to else message.from
        val currentList = _conversations.value.toMutableList()
        val existing = currentList.find { it.peerJid == peerJid }

        val updatedConversation = if (existing != null) {
            existing.copy(
                lastMessage   = message.body,
                lastTimestamp = message.timestamp,
                unreadCount   = if (message.isOutgoing) existing.unreadCount
                else existing.unreadCount + 1
            )
        } else {
            XmppConversation(
                peerJid       = peerJid,
                lastMessage   = message.body,
                lastTimestamp = message.timestamp,
                unreadCount   = if (message.isOutgoing) 0 else 1,
                isGroup       = joinedRooms.containsKey(peerJid)
            )
        }

        if (existing != null) {
            currentList[currentList.indexOf(existing)] = updatedConversation
        } else {
            currentList.add(0, updatedConversation)
        }

        currentList.sortByDescending { it.lastTimestamp }
        _conversations.value = currentList

        try {
            val db = LoquaceDatabase.getInstance(appContext)
            db.xmppConversationDao().upsert(
                XmppConversationEntity(
                    peerJid       = updatedConversation.peerJid,
                    lastMessage   = updatedConversation.lastMessage,
                    lastTimestamp = updatedConversation.lastTimestamp,
                    unreadCount   = updatedConversation.unreadCount,
                    isGroup       = updatedConversation.isGroup
                )
            )
            Log.d(TAG, "Conversation persisted to DB: $peerJid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist conversation: ${e.message}")
        }
    }

    fun getMessagesForConversation(peerJid: String): List<XmppMessage> {
        return _messages.value[peerJid] ?: emptyList()
    }

    private suspend fun storeMessage(message: XmppMessage) {
        val peerJid = if (message.isOutgoing) message.to else message.from
        Log.d(TAG, "Storing message, peerJid=$peerJid, to=${message.to}, from=${message.from}, isOutgoing=${message.isOutgoing}")
        val current = _messages.value.toMutableMap()
        val conversationMessages = (current[peerJid] ?: emptyList()).toMutableList()
        conversationMessages.add(message)
        current[peerJid] = conversationMessages
        _messages.value = current
    }

    private fun detectAttachmentType(body: String): AttachmentType {
        if (!body.startsWith("https://") && !body.startsWith("http://")) return AttachmentType.NONE
        val urlWithoutQuery = body.substringBefore("?")
        val lower = urlWithoutQuery.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".gif") ||
                    lower.endsWith(".webp") -> AttachmentType.IMAGE
            lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                    lower.endsWith(".avi") || lower.endsWith(".mov") -> AttachmentType.VIDEO
            lower.endsWith(".mp3") || lower.endsWith(".wav") ||
                    lower.endsWith(".ogg") || lower.endsWith(".mka") ||
                    lower.endsWith(".m4a") || lower.endsWith(".aac") -> AttachmentType.VOICE_NOTE
            else -> AttachmentType.FILE
        }
    }

    fun addPendingMessage(message: XmppMessage) {
        scope.launch {
            val peerJid = message.to
            val current = _messages.value.toMutableMap()
            val conversationMessages = (current[peerJid] ?: emptyList()).toMutableList()
            conversationMessages.add(message)
            current[peerJid] = conversationMessages
            _messages.value = current
        }
    }

    fun updateMessage(messageId: String, peerJid: String, updatedMessage: XmppMessage) {
        scope.launch {
            val current = _messages.value.toMutableMap()
            val conversationMessages = (current[peerJid] ?: emptyList()).toMutableList()
            val index = conversationMessages.indexOfFirst { it.id == messageId }
            if (index != -1) {
                conversationMessages[index] = updatedMessage
                current[peerJid] = conversationMessages
                _messages.value = current
            }

            val currentConvList = _conversations.value.toMutableList()
            val existingConv = currentConvList.find { it.peerJid == peerJid }
            if (existingConv != null) {
                val updated = existingConv.copy(
                    lastMessage   = updatedMessage.attachmentName ?: updatedMessage.body,
                    lastTimestamp = updatedMessage.timestamp
                )
                currentConvList[currentConvList.indexOf(existingConv)] = updated
                _conversations.value = currentConvList
            }
        }
    }

    fun deleteConversation(peerJid: String) {
        scope.launch {
            // Remove from StateFlow
            val currentList = _conversations.value.toMutableList()
            currentList.removeAll { it.peerJid == peerJid }
            _conversations.value = currentList

            // Remove messages from memory
            val currentMessages = _messages.value.toMutableMap()
            currentMessages.remove(peerJid)
            _messages.value = currentMessages

            // Remove from DB
            try {
                val db = LoquaceDatabase.getInstance(appContext)
                db.xmppConversationDao().deleteByPeerJid(peerJid)
                Log.d(TAG, "Conversation deleted: $peerJid")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete conversation: ${e.message}")
            }
        }
    }

    private fun getMucManager(): MultiUserChatManager? {
        val conn = connection ?: return null
        return MultiUserChatManager.getInstanceFor(conn)
    }

    suspend fun joinRoom(roomJid: String, nickname: String, groupName: String? = null) {
        try {
            val mucManager = getMucManager() ?: return
            val entityBareJid = JidCreate.entityBareFrom(roomJid)
            val muc = mucManager.getMultiUserChat(entityBareJid)

            if (!muc.isJoined) {
                val resource = Resourcepart.from(nickname)
                muc.join(resource)
                Log.d(TAG, "Joined MUC room: $roomJid")
            }

            if (!roomsWithListeners.contains(roomJid)) {
                muc.addMessageListener { message ->
                    scope.launch {
                        val body = message.body ?: return@launch
                        if (body.isEmpty()) return@launch

                        val retractExtension = message.extensions.find {
                            it.namespace == "urn:xmpp:message-retract:1" && it.elementName == "retract"
                        }
                        if (retractExtension != null) {
                            val retractedId = retractExtension.toXML()
                                .toString()
                                .substringAfter("id='")
                                .substringBefore("'")
                            Log.d(TAG, "Received MUC retraction for message $retractedId")
                            retractLocalMessage(roomJid, retractedId)
                            return@launch
                        }

                        val correctionExtension = message.extensions.find {
                            it.namespace == "urn:xmpp:message-correct:0" && it.elementName == "replace"
                        }
                        if (correctionExtension != null) {
                            val correctedId = correctionExtension.toXML()
                                .toString()
                                .substringAfter("id='")
                                .substringBefore("'")
                            Log.d(TAG, "Received MUC correction for message $correctedId")
                            updateMessageBody(roomJid, correctedId, body)
                            return@launch
                        }

                        val senderNickname = message.from?.resourceOrNull?.toString()
                        val myNickname = connection?.user?.asEntityBareJidIfPossible()
                            ?.localpartOrNull?.toString()

                        if (senderNickname != null && senderNickname == myNickname) {
                            Log.d(TAG, "Ignoring own MUC message")
                            return@launch
                        }

                        val attachmentType = detectAttachmentType(body)
                        val attachmentName = if (attachmentType != AttachmentType.NONE) {
                            body.substringAfterLast("/").substringBefore("?")
                        } else null

                        val xmppMessage = XmppMessage(
                            id             = message.stanzaId ?: System.currentTimeMillis().toString(),
                            from           = roomJid,
                            to             = roomJid,
                            body           = if (attachmentType != AttachmentType.NONE) "" else body,
                            timestamp      = System.currentTimeMillis(),
                            isOutgoing     = false,
                            attachmentUrl  = if (attachmentType != AttachmentType.NONE) body else null,
                            attachmentType = attachmentType,
                            attachmentName = attachmentName,
                            senderName     = senderNickname
                        )

                        Log.d(TAG, "Received MUC message in $roomJid from $senderNickname")
                        updateConversationWithMessage(xmppMessage)
                    }
                }
                roomsWithListeners.add(roomJid)
            }

            joinedRooms[roomJid] = muc
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join room $roomJid: ${e.message}")
        }
    }

    suspend fun fetchMessageHistory(
        peerJid: String,
        isGroup: Boolean,
        before: String? = null,
        limit: Int = 50
    ): Pair<List<XmppMessage>, String?> {
        return try {
            Log.d(TAG, "Fetching history for $peerJid, isGroup=$isGroup, before=$before, limit=$limit")

            val conn = connection ?: return Pair(emptyList(), null)
            val mamManager = MamManager.getInstanceFor(conn)

            if (!mamManager.isSupported) {
                Log.w(TAG, "MAM not supported by server")
                return Pair(emptyList(), null)
            }

            val queryArgs = MamManager.MamQueryArgs.builder()
                .setResultPageSizeTo(limit)
                .apply {
                    if (before != null) beforeUid(before) else queryLastPage()
                    if (!isGroup) limitResultsToJid(JidCreate.entityBareFrom(peerJid))
                }
                .build()

            val result = if (isGroup) {
                val roomJid = JidCreate.entityBareFrom(peerJid)
                val mucMamManager = MamManager.getInstanceFor(conn, roomJid)
                if (!mucMamManager.isSupported) {
                    Log.w(TAG, "MAM not supported by room $peerJid")
                    return Pair(emptyList(), null)
                }
                mucMamManager.queryArchive(queryArgs)
            } else {
                mamManager.queryArchive(queryArgs)
            }

            Log.d(TAG, "MAM result: ${result.messages.size} messages, ${result.mamResultExtensions.size} extensions")

            val myJid = conn.user.asEntityBareJidIfPossible()?.toString() ?: ""

            val rawMessages = result.messages.mapNotNull { message ->
                val body = message.body ?: return@mapNotNull null
                if (body.isEmpty()) return@mapNotNull null
                val fromJid = message.from?.asEntityBareJidIfPossible()?.toString() ?: return@mapNotNull null
                val toJid = if (isGroup) peerJid else {
                    message.to?.asEntityBareJidIfPossible()?.toString() ?: return@mapNotNull null
                }

                val isOutgoing = if (isGroup) {
                    val senderNickname = message.from?.resourceOrNull?.toString()
                    val myNickname = connection?.user?.asEntityBareJidIfPossible()?.localpartOrNull?.toString()
                    senderNickname == myNickname
                } else {
                    fromJid.substringBefore("/") == myJid.substringBefore("/")
                }

                val timestamp = message.getExtension<org.jivesoftware.smackx.delay.packet.DelayInformation>(
                    org.jivesoftware.smackx.delay.packet.DelayInformation.ELEMENT,
                    org.jivesoftware.smackx.delay.packet.DelayInformation.NAMESPACE
                )?.stamp?.time ?: System.currentTimeMillis()

                val retractExtension = message.extensions.find {
                    it.namespace == "urn:xmpp:message-retract:1" && it.elementName == "retract"
                }
                if (retractExtension != null) {
                    val retractedId = retractExtension.toXML().toString()
                        .substringAfter("id='").substringBefore("'")
                    return@mapNotNull XmppMessage(
                        id             = retractedId,
                        from           = if (isGroup) peerJid else fromJid,
                        to             = if (isGroup) peerJid else toJid,
                        body           = "This message was deleted",
                        timestamp      = timestamp,
                        isOutgoing     = isOutgoing,
                        isRetracted    = true,
                        attachmentUrl  = null,
                        attachmentType = AttachmentType.NONE,
                        attachmentName = null
                    )
                }

                val attachmentType = detectAttachmentType(body)
                val attachmentName = if (attachmentType != AttachmentType.NONE) {
                    body.substringAfterLast("/").substringBefore("?")
                } else null

                XmppMessage(
                    id             = message.stanzaId ?: System.currentTimeMillis().toString(),
                    from           = if (isGroup) peerJid else fromJid,
                    to             = if (isGroup) peerJid else toJid,
                    body           = if (attachmentType != AttachmentType.NONE) "" else body,
                    timestamp      = timestamp,
                    isOutgoing     = isOutgoing,
                    attachmentUrl  = if (attachmentType != AttachmentType.NONE) body else null,
                    attachmentType = attachmentType,
                    attachmentName = attachmentName,
                    senderName     = if (isGroup) message.from?.resourceOrNull?.toString() else null
                )
            }

            val retracted = rawMessages.filter { it.isRetracted }
            val normal = rawMessages.filter { !it.isRetracted }.toMutableList()
            for (retraction in retracted) {
                val index = normal.indexOfFirst { it.id == retraction.id }
                if (index != -1) {
                    normal[index] = normal[index].copy(body = "This message was deleted", isRetracted = true)
                }
            }

            val firstUid = result.mamResultExtensions.firstOrNull()?.id
            Log.d(TAG, "Fetched ${normal.size} history messages for $peerJid, firstUid=$firstUid")
            Pair(normal, firstUid)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch message history: ${e.message}")
            Pair(emptyList(), null)
        }
    }

    fun prependMessages(peerJid: String, messages: List<XmppMessage>) {
        scope.launch {
            val current = _messages.value.toMutableMap()
            val existing = (current[peerJid] ?: emptyList()).toMutableList()
            val existingIds = existing.map { it.id }.toSet()
            val newMessages = messages.filter { !existingIds.contains(it.id) }
            val combined = (newMessages + existing).distinctBy { it.id }
            current[peerJid] = combined
            _messages.value = current
        }
    }

    fun editMessage(toJid: String, message: XmppMessage, newBody: String, isGroup: Boolean) {
        try {
            val conn = connection ?: return
            val type = if (isGroup) org.jivesoftware.smack.packet.Message.Type.groupchat
            else org.jivesoftware.smack.packet.Message.Type.chat

            val correction = org.jivesoftware.smack.packet.Message(
                JidCreate.entityBareFrom(toJid), type
            ).apply {
                body = newBody
                addExtension(object : org.jivesoftware.smack.packet.ExtensionElement {
                    override fun getNamespace() = "urn:xmpp:message-correct:0"
                    override fun getElementName() = "replace"
                    override fun toXML(p0: org.jivesoftware.smack.packet.XmlEnvironment?): CharSequence =
                        "<replace id='${message.id}' xmlns='urn:xmpp:message-correct:0'/>"
                    override fun toXML(enclosingNamespace: String?): CharSequence =
                        "<replace id='${message.id}' xmlns='urn:xmpp:message-correct:0'/>"
                    override fun toXML(): CharSequence =
                        "<replace id='${message.id}' xmlns='urn:xmpp:message-correct:0'/>"
                })
            }
            conn.sendStanza(correction)
            Log.d(TAG, "Sent correction stanza: ${correction.toXML()}")

            scope.launch {
                val current = _messages.value.toMutableMap()
                val conversationMessages = (current[toJid] ?: emptyList()).toMutableList()
                val index = conversationMessages.indexOfFirst { it.id == message.id }
                if (index != -1) {
                    conversationMessages[index] = message.copy(body = newBody)
                    current[toJid] = conversationMessages
                    _messages.value = current
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit message: ${e.message}")
        }
    }

    fun retractMessage(toJid: String, message: XmppMessage, isGroup: Boolean) {
        try {
            val conn = connection ?: return
            val type = if (isGroup) org.jivesoftware.smack.packet.Message.Type.groupchat
            else org.jivesoftware.smack.packet.Message.Type.chat

            val retraction = org.jivesoftware.smack.packet.Message(
                JidCreate.entityBareFrom(toJid), type
            ).apply {
                body = "This message was deleted"
                addExtension(object : org.jivesoftware.smack.packet.ExtensionElement {
                    override fun getNamespace() = "urn:xmpp:message-retract:1"
                    override fun getElementName() = "retract"
                    override fun toXML(p0: org.jivesoftware.smack.packet.XmlEnvironment?): CharSequence =
                        "<retract id='${message.id}' xmlns='urn:xmpp:message-retract:1'/>"
                    override fun toXML(enclosingNamespace: String?): CharSequence =
                        "<retract id='${message.id}' xmlns='urn:xmpp:message-retract:1'/>"
                    override fun toXML(): CharSequence =
                        "<retract id='${message.id}' xmlns='urn:xmpp:message-retract:1'/>"
                })
            }
            conn.sendStanza(retraction)
            Log.d(TAG, "Sent retraction stanza: ${retraction.toXML()}")

            scope.launch { retractLocalMessage(toJid, message.id) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retract message: ${e.message}")
        }
    }

    private suspend fun retractLocalMessage(peerJid: String, messageId: String) {
        val current = _messages.value.toMutableMap()
        val conversationMessages = (current[peerJid] ?: emptyList()).toMutableList()
        val index = conversationMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            conversationMessages[index] = conversationMessages[index].copy(
                body           = "This message was deleted",
                isRetracted    = true,
                attachmentUrl  = null,
                attachmentType = AttachmentType.NONE,
                attachmentName = null,
                localPath      = null
            )
            current[peerJid] = conversationMessages
            _messages.value = current
        }
    }

    private suspend fun updateMessageBody(peerJid: String, messageId: String, newBody: String) {
        val current = _messages.value.toMutableMap()
        val conversationMessages = (current[peerJid] ?: emptyList()).toMutableList()
        val index = conversationMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            conversationMessages[index] = conversationMessages[index].copy(body = newBody)
            current[peerJid] = conversationMessages
            _messages.value = current
        }
    }

    suspend fun markConversationAsRead(peerJid: String) {
        val currentList = _conversations.value.toMutableList()
        val existing = currentList.find { it.peerJid == peerJid } ?: return
        val updated = existing.copy(unreadCount = 0)
        currentList[currentList.indexOf(existing)] = updated
        _conversations.value = currentList

        try {
            val db = LoquaceDatabase.getInstance(appContext)
            db.xmppConversationDao().upsert(
                XmppConversationEntity(
                    peerJid       = updated.peerJid,
                    lastMessage   = updated.lastMessage,
                    lastTimestamp = updated.lastTimestamp,
                    unreadCount   = 0,
                    isGroup       = updated.isGroup
                )
            )
            Log.d(TAG, "Marked conversation $peerJid as read")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark conversation as read: ${e.message}")
        }
    }

    fun updateConversationDisplayName(peerJid: String, displayName: String) {
        scope.launch {
            val currentList = _conversations.value.toMutableList()
            val existing = currentList.find { it.peerJid == peerJid } ?: return@launch
            val updated = existing.copy(displayName = displayName)
            currentList[currentList.indexOf(existing)] = updated
            _conversations.value = currentList

            try {
                val db = LoquaceDatabase.getInstance(appContext)
                db.xmppConversationDao().upsert(
                    XmppConversationEntity(
                        peerJid       = updated.peerJid,
                        lastMessage   = updated.lastMessage,
                        lastTimestamp = updated.lastTimestamp,
                        unreadCount   = updated.unreadCount,
                        isGroup       = updated.isGroup,
                        displayName   = updated.displayName
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update display name: ${e.message}")
            }
        }
    }
}