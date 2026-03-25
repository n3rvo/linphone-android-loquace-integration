package org.linphone.loquace_integration.xmpp

data class XmppMessage(
    val id: String,
    val from: String,
    val to: String,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val attachmentName: String? = null,
    var localPath: String? = null,
    val isUploading: Boolean = false  // Add this
) {
    val formattedTime: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }

    val hasAttachment: Boolean
        get() = attachmentType != AttachmentType.NONE && !attachmentUrl.isNullOrEmpty()

    val isImage: Boolean get() = attachmentType == AttachmentType.IMAGE
    val isVideo: Boolean get() = attachmentType == AttachmentType.VIDEO
    val isVoiceNote: Boolean get() = attachmentType == AttachmentType.VOICE_NOTE
    val isFile: Boolean get() = attachmentType == AttachmentType.FILE || attachmentType == AttachmentType.AUDIO
}