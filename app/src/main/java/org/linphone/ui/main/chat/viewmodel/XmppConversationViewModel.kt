package org.linphone.ui.main.chat.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    val messageSentEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    fun initialize(jid: String, name: String, group: Boolean = false) {
        peerJid.value = jid
        displayName.value = name
        isGroup.value = group

        // Load existing messages
        messages.value = LoquaceXmppManager.getMessagesForConversation(jid)
        Log.d(TAG, "Loaded ${messages.value?.size} messages for $jid")

        // Observe new incoming messages
        viewModelScope.launch {
            LoquaceXmppManager.messages.collectLatest { allMessages ->
                val conversationMessages = allMessages[jid] ?: emptyList()
                messages.postValue(conversationMessages)
            }
        }
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
}