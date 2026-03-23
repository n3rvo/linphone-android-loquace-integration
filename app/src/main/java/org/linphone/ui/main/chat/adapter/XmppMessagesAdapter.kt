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

class XmppMessagesAdapter : ListAdapter<XmppMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val INCOMING = 0
        private const val OUTGOING = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isOutgoing) OUTGOING else INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == OUTGOING) {
            val binding: LoquaceChatBubbleOutgoingBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.loquace_chat_bubble_outgoing,
                parent,
                false
            )
            OutgoingViewHolder(binding)
        } else {
            val binding: LoquaceChatBubbleIncomingBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.loquace_chat_bubble_incoming,
                parent,
                false
            )
            IncomingViewHolder(binding)
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
        }
    }

    inner class IncomingViewHolder(val binding: LoquaceChatBubbleIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(message: XmppMessage) {
            binding.model = message
            binding.executePendingBindings()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<XmppMessage>() {
        override fun areItemsTheSame(oldItem: XmppMessage, newItem: XmppMessage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: XmppMessage, newItem: XmppMessage) =
            oldItem == newItem
    }
}