package org.linphone.ui.main.chat.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.core.tools.Log
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager
import org.linphone.ui.main.chat.model.XmppConversationModel
import org.linphone.ui.main.viewmodel.AbstractMainViewModel
import org.linphone.utils.Event

class XmppConversationsListViewModel
@UiThread
constructor() : AbstractMainViewModel() {

    companion object {
        private const val TAG = "[Xmpp Conversations List ViewModel]"
    }

    enum class ChatTab { CHATS, CONTACTS, GROUPS }

    val currentTab = MutableLiveData<ChatTab>(ChatTab.CHATS)

    val conversations = MutableLiveData<List<XmppConversationModel>>()

    val fetchInProgress = MutableLiveData<Boolean>(false)

    val isListEmpty = MutableLiveData<Boolean>(true)

    val openConversationEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    init {
        viewModelScope.launch {
            LoquaceXmppManager.conversations.collectLatest { xmppConversations ->
                Log.d(TAG, "Conversations updated: ${xmppConversations.size} items")
                coreContext.postOnCoreThread {
                    val models = xmppConversations.map { XmppConversationModel(it) }
                    conversations.postValue(models)
                    isListEmpty.postValue(models.isEmpty())
                }
            }
        }
    }

    @UiThread
    fun switchTab(tab: ChatTab) {
        currentTab.value = tab
    }

    @UiThread
    fun openConversationWith(peerJid: String) {
        openConversationEvent.value = Event(peerJid)
    }

    @UiThread
    override fun filter() {
        // Search filtering will be added in a later phase
    }
}