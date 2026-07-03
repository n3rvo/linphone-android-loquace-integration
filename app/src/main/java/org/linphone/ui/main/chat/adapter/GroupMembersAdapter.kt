package org.linphone.ui.main.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import org.linphone.R
import org.linphone.core.Participant
import org.linphone.databinding.LoquaceGroupMemberCellBinding
import org.linphone.loquace_integration.network.GroupParticipant
import org.linphone.utils.Event
import org.linphone.utils.setLoquacePresenceRing

class GroupMembersAdapter(
    private val isOwner: Boolean,
) : ListAdapter<GroupParticipant, GroupMembersAdapter.ViewHolder>(DiffCallback()) {

    private val avatarBitmaps = mutableMapOf<String, android.graphics.Bitmap>()
    private val presenceStatuses = mutableMapOf<String, String?>()

    val removeClickedEvent: MutableLiveData<Event<GroupParticipant>> by lazy {
        MutableLiveData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: LoquaceGroupMemberCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.loquace_group_member_cell,
            parent,
            false
        )
        val viewHolder = ViewHolder(binding)
        binding.apply {
            this.isOwner = this@GroupMembersAdapter.isOwner
            removeButton.setOnClickListener {
                removeClickedEvent.value = Event(viewHolder.binding.participant!!)
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(val binding: LoquaceGroupMemberCellBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(participant: GroupParticipant) {
            binding.participant = participant

            binding.roleBadge.text = binding.root.context.getString(R.string.group_member)
            if (participant.role == "owner"){
                binding.roleBadge.text = binding.root.context.getString(R.string.group_owner)
                binding.removeButton.visibility = View.INVISIBLE
                binding.removeButton.isClickable = false
            } else {
                if (isOwner) {
                    binding.removeButton.visibility = View.VISIBLE
                    binding.removeButton.isClickable = true
                }
            }

            // Clip avatar container to circle
            binding.avatarContainer.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            binding.avatarContainer.clipToOutline = true

            val name = participant.fullName ?: participant.account
            val initials = name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .uppercase()

            val bitmap = avatarBitmaps[participant.account]
            if (bitmap != null) {
                binding.avatar.setImageBitmap(bitmap)
                binding.avatar.visibility = View.VISIBLE
                binding.avatarInitials.visibility = View.GONE
            } else {
                binding.avatarInitials.text = initials
                binding.avatarInitials.visibility = View.VISIBLE
                binding.avatar.visibility = View.GONE
            }

            // Apply presence ring
            binding.presenceRing.setLoquacePresenceRing(presenceStatuses[participant.account])

            binding.executePendingBindings()
        }
    }

    fun updateAvatar(account: String, bitmap: android.graphics.Bitmap) {
        avatarBitmaps[account] = bitmap
        // Find the position of this participant and rebind
        val position = currentList.indexOfFirst { it.account == account }
        if (position != -1) notifyItemChanged(position)
    }

    fun updatePresence(account: String, status: String?) {
        presenceStatuses[account] = status
        val position = currentList.indexOfFirst { it.account == account }
        if (position != -1) notifyItemChanged(position)
    }

    private class DiffCallback : DiffUtil.ItemCallback<GroupParticipant>() {
        override fun areItemsTheSame(oldItem: GroupParticipant, newItem: GroupParticipant) =
            oldItem.account == newItem.account
        override fun areContentsTheSame(oldItem: GroupParticipant, newItem: GroupParticipant) =
            oldItem == newItem
    }
}