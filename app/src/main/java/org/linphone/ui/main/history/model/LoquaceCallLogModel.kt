package org.linphone.ui.main.history.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.ui.main.contacts.model.ContactAvatarModel
import org.linphone.utils.AppUtils
import org.linphone.utils.TimestampUtils
import org.linphone.loquace_integration.network.CallHistoryResponse
import org.linphone.utils.FileUtils

class LoquaceCallLogModel
@WorkerThread
constructor(val call: CallHistoryResponse, val avatarPath: String? = null) {

    val id = call.id

    val timestamp = call.dateTime

    val contactName = call.contact?.name ?: call.account ?: ""

    val number = call.contact?.number ?: call.account ?: ""

    val missed = call.missed

    val seen = call.seen

    val avatarModel: ContactAvatarModel

    val iconResId: Int

    val dateTime: String

    init {
        val date = if (TimestampUtils.isToday(timestamp)) {
            AppUtils.getString(R.string.today)
        } else if (TimestampUtils.isYesterday(timestamp)) {
            AppUtils.getString(R.string.yesterday)
        } else {
            TimestampUtils.toString(timestamp, onlyDate = true, shortDate = true, hideYear = true)
        }
        val time = TimestampUtils.timeToString(timestamp)
        dateTime = "$date | $time"

        val friend = coreContext.core.createFriend()
        friend.name = contactName
        friend.refKey = call.contact?.id
        if (number.isNotEmpty()) friend.addPhoneNumber(number)
        if (!avatarPath.isNullOrEmpty()) {
            friend.photo = FileUtils.getProperFilePath(avatarPath)
        }
        avatarModel = coreContext.contactsManager.getContactAvatarModelForFriend(friend)

        iconResId = when (call.direction) {
            "OUTGOING" -> R.drawable.outgoing_call
            "INCOMING" -> if (call.missed) R.drawable.incoming_call_missed else R.drawable.incoming_call
            else -> R.drawable.outgoing_call
        }
    }
}