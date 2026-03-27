package org.linphone.ui.main.chat.model

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.loquace_integration.xmpp.XmppConversation
import org.linphone.ui.main.contacts.model.ContactAvatarModel
import org.linphone.utils.TimestampUtils
import org.linphone.utils.AppUtils
import org.linphone.R

class XmppConversationModel
@WorkerThread
constructor(
    val conversation: XmppConversation,
    prebuiltAvatarModel: ContactAvatarModel? = null
) {

    val id = conversation.peerJid

    val isGroup = conversation.isGroup

    val subject = conversation.displayName ?: conversation.peerJid

    val lastMessageText = MutableLiveData<String>(conversation.lastMessage)

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
            friend.name = conversation.displayName
                ?: conversation.peerJid.substringBefore("@")
            friend.refKey = conversation.peerJid
            coreContext.contactsManager.getContactAvatarModelForFriend(friend)
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