package org.linphone.ui.main.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.databinding.LoquaceChatBubbleIncomingBinding
import org.linphone.databinding.LoquaceChatBubbleOutgoingBinding
import org.linphone.loquace_integration.xmpp.XmppMessage
import coil3.load
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.loquace_integration.network.LoquaceMediaDownloader
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.utils.Event
import java.io.File

class XmppMessagesAdapter : ListAdapter<XmppMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val INCOMING = 0
        private const val OUTGOING = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isOutgoing) OUTGOING else INCOMING
    }

    val attachmentClickedEvent: MutableLiveData<Event<XmppMessage>> by lazy {
        MutableLiveData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == OUTGOING) {
            val binding: LoquaceChatBubbleOutgoingBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.loquace_chat_bubble_outgoing,
                parent,
                false
            )
            val viewHolder = OutgoingViewHolder(binding)
            binding.apply {
                lifecycleOwner = parent.findViewTreeLifecycleOwner()
                attachmentImage.setOnClickListener {
                    attachmentClickedEvent.value = Event(viewHolder.binding.model!!)
                }
                attachmentVideo.setOnClickListener {
                    attachmentClickedEvent.value = Event(viewHolder.binding.model!!)
                }
                attachmentFile.setOnClickListener {
                    attachmentClickedEvent.value = Event(viewHolder.binding.model!!)
                }
            }
            viewHolder
        } else {
            val binding: LoquaceChatBubbleIncomingBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.loquace_chat_bubble_incoming,
                parent,
                false
            )
            val viewHolder = IncomingViewHolder(binding)
            binding.apply {
                lifecycleOwner = parent.findViewTreeLifecycleOwner()
                attachmentImage.setOnClickListener {
                    attachmentClickedEvent.value = Event(viewHolder.binding.model!!)
                }
                attachmentVideo.setOnClickListener {
                    attachmentClickedEvent.value = Event(viewHolder.binding.model!!)
                }
                attachmentFile.setOnClickListener {
                    attachmentClickedEvent.value = Event(viewHolder.binding.model!!)
                }
            }
            viewHolder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is OutgoingViewHolder) holder.bind(message)
        else (holder as IncomingViewHolder).bind(message)
    }

    inner class OutgoingViewHolder(val binding: LoquaceChatBubbleOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(message: XmppMessage) {
            binding.model = message
            binding.executePendingBindings()

            Log.d("XmppAdapter", "Message: body=${message.body}, isImage=${message.isImage}, isVideo=${message.isVideo}, isFile=${message.isFile}, attachmentUrl=${message.attachmentUrl}, attachmentType=${message.attachmentType}")

            binding.attachmentImage.visibility = if (message.isImage) View.VISIBLE else View.GONE
            binding.attachmentVideo.visibility = if (message.isVideo) View.VISIBLE else View.GONE
            binding.attachmentFile.visibility = if (message.isFile) View.VISIBLE else View.GONE
            binding.attachmentVoice.visibility = if (message.isVoiceNote) View.VISIBLE else View.GONE
            binding.textContent.visibility = if (message.body.isNotEmpty()) View.VISIBLE else View.GONE
            binding.uploadProgress.visibility = if (message.isUploading) View.VISIBLE else View.GONE

            Log.d("XmppAdapter", "attachmentImage visibility=${binding.attachmentImage.visibility}")

            loadAttachment(message, binding)
        }
    }

    inner class IncomingViewHolder(val binding: LoquaceChatBubbleIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(message: XmppMessage) {
            binding.model = message
            binding.executePendingBindings()

            Log.d("XmppAdapter", "Message: body=${message.body}, isImage=${message.isImage}, isVideo=${message.isVideo}, isFile=${message.isFile}, attachmentUrl=${message.attachmentUrl}, attachmentType=${message.attachmentType}")

            binding.attachmentImage.visibility = if (message.isImage) View.VISIBLE else View.GONE
            binding.attachmentVideo.visibility = if (message.isVideo) View.VISIBLE else View.GONE
            binding.attachmentFile.visibility = if (message.isFile) View.VISIBLE else View.GONE
            binding.attachmentVoice.visibility = if (message.isVoiceNote) View.VISIBLE else View.GONE
            binding.textContent.visibility = if (message.body.isNotEmpty()) View.VISIBLE else View.GONE
            binding.uploadProgress.visibility = if (message.isUploading) View.VISIBLE else View.GONE

            Log.d("XmppAdapter", "attachmentImage visibility=${binding.attachmentImage.visibility}")


            loadAttachment(message, binding)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<XmppMessage>() {
        override fun areItemsTheSame(oldItem: XmppMessage, newItem: XmppMessage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: XmppMessage, newItem: XmppMessage) =
            oldItem == newItem
    }

    private fun loadAttachment(message: XmppMessage, binding: Any) {
        val imageView = when (binding) {
            is LoquaceChatBubbleIncomingBinding -> binding.attachmentImage
            is LoquaceChatBubbleOutgoingBinding -> binding.attachmentImage
            else -> return
        }
        val videoThumb = when (binding) {
            is LoquaceChatBubbleIncomingBinding -> binding.videoThumbnail
            is LoquaceChatBubbleOutgoingBinding -> binding.videoThumbnail
            else -> null
        }
        val voicePlayButton = when (binding) {
            is LoquaceChatBubbleIncomingBinding -> binding.voicePlayButton
            is LoquaceChatBubbleOutgoingBinding -> binding.voicePlayButton
            else -> null
        }

        when {
            message.isImage -> {
                val url = message.attachmentUrl
                if (url == null) {
                    Log.d("XmppAdapter", "Video still uploading, skipping thumbnail")
                    return
                }

                val token = org.linphone.loquace_integration.storage.SessionManager(imageView.context).getToken() ?: ""
                val domain = org.linphone.loquace_integration.storage.SessionManager(imageView.context).getDomain() ?: ""

                CoroutineScope(Dispatchers.IO).launch {
                    val bytes = LoquaceMediaDownloader.downloadBytes(
                        url    = message.attachmentUrl!!,
                        token  = token,
                        domain = domain
                    )
                    if (bytes != null) {
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        withContext(Dispatchers.Main) {
                            imageView.setImageBitmap(bitmap)
                            Log.d("XmppAdapter", "Image loaded successfully")
                        }
                    } else {
                        Log.e("XmppAdapter", "Failed to download image")
                    }
                }
            }
            message.isVideo -> {
                val url = message.attachmentUrl ?: return
                val token = SessionManager(imageView.context).getToken() ?: ""
                val domain = SessionManager(imageView.context).getDomain() ?: ""

                CoroutineScope(Dispatchers.IO).launch {
                    val cachedFile = File(imageView.context.cacheDir, "video_${message.id}.mp4")

                    if (!cachedFile.exists()) {
                        val bytes = LoquaceMediaDownloader.downloadBytes(url, token, domain)
                        if (bytes != null) {
                            cachedFile.writeBytes(bytes)
                            message.localPath = cachedFile.absolutePath
                        }
                    } else {
                        message.localPath = cachedFile.absolutePath
                    }

                    if (cachedFile.exists()) {
                        val thumb = ThumbnailUtils.createVideoThumbnail(
                            cachedFile.absolutePath,
                            MediaStore.Images.Thumbnails.MINI_KIND
                        )
                        withContext(Dispatchers.Main) {
                            videoThumb?.setImageBitmap(thumb)
                        }
                    }
                }
            }
            message.isVoiceNote -> {
                voicePlayButton?.setOnClickListener {
                    // Voice note playback will be implemented next
                }
            }
        }
    }

}