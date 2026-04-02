package org.linphone.ui.main.chat.model

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.loquace_integration.xmpp.XmppConversation
import org.linphone.ui.main.contacts.model.ContactAvatarModel
import org.linphone.utils.TimestampUtils
import org.linphone.utils.AppUtils
import org.linphone.R
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager

class XmppConversationModel
@WorkerThread
constructor(
    val conversation: XmppConversation,
    prebuiltAvatarModel: ContactAvatarModel? = null
) {

    val id = conversation.peerJid

    val isGroup = conversation.isGroup

    val subject = conversation.displayName
        ?: LoquaceXmppManager.getContactName(conversation.peerJid)
        ?: conversation.peerJid

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
                ?: LoquaceXmppManager.getContactName(conversation.peerJid)
                        ?: conversation.peerJid.substringBefore("@")
            friend.refKey = conversation.peerJid

            val contactId = LoquaceXmppManager.getContactId(conversation.peerJid)
            if (contactId != null) {
                val avatarFile = java.io.File(
                    coreContext.context.filesDir,
                    "avatar_$contactId.jpg"
                )
                if (avatarFile.exists()) {
                    // Use cached avatar
                    friend.photo = avatarFile.absolutePath
                } else {
                    // Download in background
                    val pictureUrl = LoquaceXmppManager.getContactPictureUrl(conversation.peerJid)
                    if (pictureUrl != null) {
                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val sessionManager = org.linphone.loquace_integration.storage.SessionManager(coreContext.context)
                            val token = sessionManager.getToken() ?: return@launch
                            val domain = sessionManager.getDomain() ?: return@launch
                            val bytes = org.linphone.loquace_integration.network.LoquaceMediaDownloader.downloadBytes(
                                url    = pictureUrl,
                                token  = token,
                                domain = domain
                            )
                            if (bytes != null) {
                                avatarFile.writeBytes(bytes)
                                friend.photo = avatarFile.absolutePath
                            }
                        }
                    }
                }
            }

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