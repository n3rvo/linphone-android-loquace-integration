package org.linphone.ui.main.chat.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.core.tools.Log
import org.linphone.loquace_integration.network.LoquaceAvatarHelper
import org.linphone.loquace_integration.network.LoquaceConfig
import org.linphone.loquace_integration.network.LoquaceGroupsRepository
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager
import org.linphone.loquace_integration.xmpp.XmppConversation
import org.linphone.ui.main.chat.model.XmppConversationModel
import org.linphone.ui.main.viewmodel.AbstractMainViewModel
import org.linphone.utils.Event
import org.linphone.utils.FileUtils
import java.io.File

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

    val contacts = MutableLiveData<List<XmppConversationModel>>()
    val isFetchingContacts = MutableLiveData<Boolean>(false)

    val groups = MutableLiveData<List<XmppConversationModel>>()
    val isFetchingGroups = MutableLiveData<Boolean>(false)

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
    override fun filter() {}

    fun loadContacts(domain: String, token: String, userAgent: String, filesDir: File) {
        if (isFetchingContacts.value == true) return

        viewModelScope.launch {
            isFetchingContacts.value = true
            val repository = LoquaceGroupsRepository()
            var offset = 0
            val allModels = arrayListOf<XmppConversationModel>()

            while (true) {
                val page = repository.fetchChatEnabledContacts(
                    domain    = domain,
                    token     = token,
                    userAgent = userAgent,
                    offset    = offset
                )
                if (page.isEmpty()) break

                val avatarPaths = mutableMapOf<String, String>()
                for (contact in page) {
                    val path = LoquaceAvatarHelper.fetchAndSaveAvatar(
                        contactId  = contact.id,
                        pictureUrl = contact.pictureUrl,
                        domain     = domain,
                        token      = token,
                        userAgent  = userAgent,
                        filesDir   = filesDir
                    )
                    if (path != null) avatarPaths[contact.id] = path
                }

                coreContext.postOnCoreThread {
                    val newModels = page.map { contact ->
                        val jid = contact.chats?.firstOrNull { it.type == "xmpp" }?.account
                            ?: return@map null

                        val friend = coreContext.core.createFriend()
                        friend.name = contact.fullName
                            ?: "${contact.firstName} ${contact.lastName}".trim()
                        friend.refKey = contact.id
                        avatarPaths[contact.id]?.let {
                            friend.photo = FileUtils.getProperFilePath(it)
                        }
                        val avatarModel = coreContext.contactsManager
                            .getContactAvatarModelForFriend(friend)

                        XmppConversationModel(
                            conversation = XmppConversation(
                                peerJid       = jid,
                                lastMessage   = "",
                                lastTimestamp = 0L,
                                isGroup       = false
                            ),
                            prebuiltAvatarModel = avatarModel,
                            displayName = MutableLiveData(friend.name)
                        )
                    }.filterNotNull()

                    allModels.addAll(newModels)
                    contacts.postValue(ArrayList(allModels))
                }

                if (page.size < LoquaceConfig.CONTACTS_PAGE_SIZE) break
                offset += page.size
            }

            isFetchingContacts.postValue(false)
        }
    }

    fun loadGroups(domain: String, token: String, userAgent: String) {
        if (isFetchingGroups.value == true) return
        viewModelScope.launch {
            isFetchingGroups.value = true
            val repository = LoquaceGroupsRepository()
            val groupList = repository.getGroups(domain, token, userAgent)
            Log.d(TAG, "Fetched ${groupList.size} groups")

            coreContext.postOnCoreThread {
                val models = groupList.map { group ->
                    XmppConversationModel(
                        conversation = XmppConversation(
                            peerJid       = group.jid,
                            lastMessage   = "${group.participants.size} members",
                            lastTimestamp = group.createdAt,
                            isGroup       = true
                        ),
                        displayName = MutableLiveData(group.name)
                    )
                }
                groups.postValue(models)
                isFetchingGroups.postValue(false)
            }
        }
    }
}