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
import org.linphone.utils.Event

class XmppConversationViewModel
@UiThread
constructor() : GenericViewModel() {

    companion object {
        private const val TAG = "[Xmpp Conversation ViewModel]"
    }

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
}