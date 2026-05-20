package org.linphone.ui.main.chat.model

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.loquace_integration.xmpp.XmppConversation
import org.linphone.ui.main.contacts.model.ContactAvatarModel
import org.linphone.utils.TimestampUtils
import org.linphone.utils.AppUtils
import org.linphone.utils.FileUtils
import org.linphone.R
import org.linphone.loquace_integration.network.LoquaceGroupsRepository
import org.linphone.loquace_integration.network.LoquaceMediaDownloader
import org.linphone.loquace_integration.storage.SessionManager

class XmppConversationModel
@WorkerThread
constructor(
    val conversation: XmppConversation,
    prebuiltAvatarModel: ContactAvatarModel? = null,
    val displayName: MutableLiveData<String> = MutableLiveData(
        conversation.peerJid.substringBefore("@")
    ),
    val loquacePresence: MutableLiveData<String?> = MutableLiveData(null)
) {
    val id = conversation.peerJid
    val isGroup = conversation.isGroup
    val lastMessageText = MutableLiveData<String?>(conversation.lastMessage)
    val lastMessageTextSender = MutableLiveData<String>("")
    val lastMessageContentIcon = MutableLiveData<Int>(0)
    val unreadMessageCount = MutableLiveData<Int>(conversation.unreadCount)
    val isLastMessageOutgoing = MutableLiveData<Boolean>(false)
    val isMuted = MutableLiveData<Boolean>(false)
    val isEphemeral = MutableLiveData<Boolean>(false)
    val isEncrypted = MutableLiveData<Boolean>(false)
    val isEncryptionAvailable = MutableLiveData<Boolean>(false)
    val isBeingDeleted = MutableLiveData<Boolean>(false)
    val isComposing = MutableLiveData<Boolean>(false)
    val composingLabel = MutableLiveData<String>("")
    val composingIcon = MutableLiveData<Int>(0)
    val lastMessageDeliveryIcon = MutableLiveData<Int>(0)
    val dateTime = MutableLiveData<String>(formatDateTime(conversation.lastTimestamp))
    val avatarModel: ContactAvatarModel

    init {
        avatarModel = prebuiltAvatarModel ?: run {
            val friend = coreContext.core.createFriend()
            friend.name = conversation.peerJid.substringBefore("@")
            friend.refKey = conversation.peerJid

            val model = coreContext.contactsManager.getContactAvatarModelForFriend(friend)

            if (!conversation.isGroup) {
                GlobalScope.launch(Dispatchers.IO) {
                    val sessionManager = SessionManager(coreContext.context)
                    val token = sessionManager.getToken() ?: return@launch
                    val domain = sessionManager.getDomain() ?: return@launch
                    val userAgent = sessionManager.getUserAgent()

                    val contact = LoquaceGroupsRepository().getContactByJid(
                        domain, token, userAgent, conversation.peerJid
                    ) ?: return@launch

                    Log.d("XmppConversationModel", "contact.id=${contact.id}, avatarFile=avatar_${contact.id}.jpg")

                    loquacePresence.postValue(contact.presence?.status)

                    // Update display name
                    val name = contact.fullName
                        ?: "${contact.firstName} ${contact.lastName}".trim()
                    displayName.postValue(name)
                    friend.name = name

                    // Download avatar
                    val pictureUrl = contact.pictureUrl ?: return@launch
                    val avatarFile = java.io.File(
                        coreContext.context.filesDir,
                        "avatar_${contact.id}.jpg"
                    )

                    Log.d("XmppConversationModel", "avatarFile path: ${avatarFile.absolutePath}, exists: ${avatarFile.exists()}")

                    if (!avatarFile.exists()) {
                        val bytes = LoquaceMediaDownloader.downloadBytes(
                            url    = pictureUrl,
                            token  = token,
                            domain = domain
                        )
                        if (bytes != null) avatarFile.writeBytes(bytes)
                    }
                    if (avatarFile.exists()) {
                        model.picturePath.postValue(
                            FileUtils.getProperFilePath(avatarFile.absolutePath)
                        )
                        Log.d("XmppConversationModel", "Posted picture path: ${avatarFile.absolutePath}")
                    }
                }
            } else {
                GlobalScope.launch(Dispatchers.IO) {
                    val sessionManager = SessionManager(coreContext.context)
                    val token = sessionManager.getToken() ?: return@launch
                    val domain = sessionManager.getDomain() ?: return@launch
                    val userAgent = sessionManager.getUserAgent()
                    val groups = LoquaceGroupsRepository().getGroups(domain, token, userAgent)
                    val group = groups.firstOrNull { it.jid == conversation.peerJid } ?: return@launch
                    displayName.postValue(group.name)
            }
        }

            model
        }
    }

    fun update(conversation: XmppConversation) {
        lastMessageText.postValue(conversation.lastMessage)
        unreadMessageCount.postValue(conversation.unreadCount)
        dateTime.postValue(formatDateTime(conversation.lastTimestamp))
    }

    fun markAsRead() {
        unreadMessageCount.postValue(0)
    }

    private fun formatDateTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return if (TimestampUtils.isToday(timestamp)) {
            TimestampUtils.timeToString(timestamp)
        } else if (TimestampUtils.isYesterday(timestamp)) {
            AppUtils.getString(R.string.yesterday)
        } else {
            TimestampUtils.toString(timestamp, onlyDate = true, shortDate = true, hideYear = true)
        }
    }
}