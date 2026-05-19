package org.linphone.ui.main.chat.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.tools.Log
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager
import org.linphone.loquace_integration.xmpp.XmppMessage
import org.linphone.ui.GenericViewModel
import org.linphone.ui.main.contacts.model.ContactAvatarModel
import org.linphone.utils.Event

class XmppConversationViewModel
@UiThread
constructor() : GenericViewModel() {

    companion object {
        private const val TAG = "[Xmpp Conversation ViewModel]"
    }

    val avatarModel = MutableLiveData<ContactAvatarModel>()
    val peerJid = MutableLiveData<String>()
    val displayName = MutableLiveData<String>()
    val messages = MutableLiveData<List<XmppMessage>>()
    val isGroup = MutableLiveData<Boolean>(false)

    val isLoadingHistory = MutableLiveData<Boolean>(false)
    val hasMoreHistory = MutableLiveData<Boolean>(true)
    private var oldestMessageUid: String? = null
    var isPrependingHistory = false

    val messageSentEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val fetchInProgress = MutableLiveData<Boolean>(false)

    fun initialize(jid: String, name: String, group: Boolean = false) {
        peerJid.value = jid
        displayName.value = name
        isGroup.value = group

        // Load avatar
        viewModelScope.launch(Dispatchers.IO) {
            val friend = org.linphone.LinphoneApplication.coreContext.core.createFriend()
            friend.name = name
            friend.refKey = jid

            if (!group) {
                val sessionManager = org.linphone.loquace_integration.storage.SessionManager(
                    org.linphone.LinphoneApplication.coreContext.context
                )
                val token = sessionManager.getToken() ?: return@launch
                val domain = sessionManager.getDomain() ?: return@launch
                val userAgent = sessionManager.getUserAgent()

                val contact = org.linphone.loquace_integration.network.LoquaceGroupsRepository()
                    .getContactByJid(domain, token, userAgent, jid)

                if (contact != null) {
                    friend.name = contact.fullName
                        ?: "${contact.firstName} ${contact.lastName}".trim()
                    val avatarFile = java.io.File(
                        org.linphone.LinphoneApplication.coreContext.context.filesDir,
                        "avatar_${contact.id}.jpg"
                    )
                    if (!avatarFile.exists()) {
                        val pictureUrl = contact.pictureUrl ?: return@launch
                        val bytes = org.linphone.loquace_integration.network.LoquaceMediaDownloader
                            .downloadBytes(url = pictureUrl, token = token, domain = domain)
                        if (bytes != null) avatarFile.writeBytes(bytes)
                    }
                    if (avatarFile.exists()) {
                        friend.photo = org.linphone.utils.FileUtils
                            .getProperFilePath(avatarFile.absolutePath)
                    }
                }
            }

            org.linphone.LinphoneApplication.coreContext.postOnCoreThread {
                val model = org.linphone.LinphoneApplication.coreContext.contactsManager
                    .getContactAvatarModelForFriend(friend)
                avatarModel.postValue(model)
            }
        }

        // Load existing in-memory messages
        messages.value = LoquaceXmppManager.getMessagesForConversation(jid)
        Log.d(TAG, "Loaded ${messages.value?.size} messages for $jid")

        // Observe new messages
        viewModelScope.launch {
            LoquaceXmppManager.messages.collectLatest { allMessages ->
                val conversationMessages = allMessages[jid] ?: emptyList()
                Log.d(TAG, "Messages updated for $jid: ${conversationMessages.size} messages")
                messages.postValue(conversationMessages)
            }
        }

        // Load history
        loadHistory(jid, group)
    }

    @UiThread
    fun sendMessage(text: String) {
        val jid = peerJid.value ?: return
        if (text.trim().isEmpty()) return

        Log.d(TAG, "Sending message to $jid: $text")
        val result = LoquaceXmppManager.sendMessage(jid, text.trim())
        if (result != null) {
            messageSentEvent.value = Event(true)
        } else {
            Log.e(TAG, "Failed to send message to $jid")
        }
    }

    fun loadHistory(jid: String, group: Boolean, before: String? = null) {
        if (isLoadingHistory.value == true) return
        viewModelScope.launch {
            isLoadingHistory.value = true
            val (history, firstUid) = withContext(Dispatchers.IO) {
                LoquaceXmppManager.fetchMessageHistory(
                    peerJid = jid,
                    isGroup = group,
                    before  = before
                )
            }

            if (history.isEmpty()) {
                hasMoreHistory.value = false
                isLoadingHistory.value = false
                return@launch
            }

            if (history.size < 50) hasMoreHistory.value = false
            oldestMessageUid = firstUid

            val current = LoquaceXmppManager.getMessagesForConversation(jid).toMutableList()
            val historyIds = current.map { it.id }.toSet()
            val newMessages = history.filter { !historyIds.contains(it.id) }

            if (newMessages.isNotEmpty()) {
                isPrependingHistory = true
                LoquaceXmppManager.prependMessages(jid, newMessages)
            }

            isLoadingHistory.value = false

            isLoadingHistory.value = false
        }
    }

    fun loadMoreHistory() {
        val jid = peerJid.value ?: return
        val group = isGroup.value ?: false
        Log.d(TAG, "loadMoreHistory called, hasMore=${hasMoreHistory.value}, isLoading=${isLoadingHistory.value}, oldestUid=$oldestMessageUid")
        if (hasMoreHistory.value == true && isLoadingHistory.value == false) {
            loadHistory(jid, group, before = oldestMessageUid)
        }
    }

    fun editMessage(message: XmppMessage, newBody: String) {
        val jid = peerJid.value ?: return
        val group = isGroup.value ?: false
        viewModelScope.launch(Dispatchers.IO) {
            LoquaceXmppManager.editMessage(
                toJid   = jid,
                message = message,
                newBody = newBody,
                isGroup = group
            )
        }
    }

    fun deleteMessage(message: XmppMessage) {
        val jid = peerJid.value ?: return
        val group = isGroup.value ?: false
        viewModelScope.launch(Dispatchers.IO) {
            LoquaceXmppManager.retractMessage(
                toJid   = jid,
                message = message,
                isGroup = group
            )
        }
    }

    fun markAsRead() {
        val jid = peerJid.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            LoquaceXmppManager.markConversationAsRead(jid)
        }
    }
}