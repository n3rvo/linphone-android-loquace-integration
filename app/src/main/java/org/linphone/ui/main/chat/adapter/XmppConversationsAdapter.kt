package org.linphone.ui.main.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.databinding.LoquaceChatListCellBinding
import org.linphone.ui.main.chat.model.XmppConversationModel
import org.linphone.utils.Event

class XmppConversationsAdapter :
    ListAdapter<XmppConversationModel, XmppConversationsAdapter.ViewHolder>(DiffCallback()) {

    val conversationClickedEvent: MutableLiveData<Event<XmppConversationModel>> by lazy {
        MutableLiveData()
    }

    val conversationLongClickedEvent: MutableLiveData<Event<XmppConversationModel>> by lazy {
        MutableLiveData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: LoquaceChatListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.loquace_chat_list_cell,
            parent,
            false
        )
        val viewHolder = ViewHolder(binding)
        binding.apply {
            lifecycleOwner = parent.findViewTreeLifecycleOwner()

            setOnClickListener {
                conversationClickedEvent.value = Event(model!!)
            }

            setOnLongClickListener {
                conversationLongClickedEvent.value = Event(model!!)
                true
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        holder.bind(model)

        // Observe avatar changes and rebind when picturePath updates
        val lifecycleOwner = holder.binding.lifecycleOwner ?: return
        model.avatarModel.picturePath.observe(lifecycleOwner) {
            holder.bind(model)
        }
    }

    inner class ViewHolder(val binding: LoquaceChatListCellBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(model: XmppConversationModel) {
            binding.model = model
            binding.executePendingBindings()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<XmppConversationModel>() {
        override fun areItemsTheSame(
            oldItem: XmppConversationModel,
            newItem: XmppConversationModel
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: XmppConversationModel,
            newItem: XmppConversationModel
        ) = oldItem.conversation == newItem.conversation
    }
}