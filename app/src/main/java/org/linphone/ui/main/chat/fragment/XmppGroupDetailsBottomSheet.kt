package org.linphone.ui.main.chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.LoquaceGroupDetailsBottomSheetBinding
import org.linphone.loquace_integration.network.GroupParticipant
import org.linphone.loquace_integration.network.GroupResponse
import org.linphone.loquace_integration.network.LoquaceConfig
import org.linphone.loquace_integration.network.LoquaceGroupsRepository
import org.linphone.loquace_integration.network.LoquaceMediaDownloader
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager
import org.linphone.ui.main.chat.adapter.GroupMembersAdapter

@UiThread
class XmppGroupDetailsBottomSheet(
    private val group: GroupResponse,
    private val onGroupDeleted: () -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "GroupDetailsBottomSheet"
    }

    private lateinit var binding: LoquaceGroupDetailsBottomSheetBinding
    private lateinit var adapter: GroupMembersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.loquace_group_details_bottom_sheet,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val domain = sessionManager.getDomain() ?: ""
        val token = sessionManager.getToken() ?: ""
        val userAgent = sessionManager.getUserAgent()

        // Determine if current user is owner
        val myJid = LoquaceXmppManager.getMyJid()
        val isOwner = group.participants.any {
            it.role == "owner" && it.account == myJid
        }

        binding.groupName.text = group.name
        binding.memberCount.text = "${group.participants.size} ${getString(R.string.group_members)}"
        binding.lifecycleOwner = viewLifecycleOwner

        binding.addMemberButton.visibility = if (isOwner) View.VISIBLE else View.GONE
        binding.deleteGroupButton.visibility = if (isOwner) View.VISIBLE else View.GONE

        binding.addMemberButton.setOnClickListener {
            showAddMemberDialog(domain, token, userAgent)
        }

        binding.deleteGroupButton.setOnClickListener {
            showDeleteConfirmation(domain, token, userAgent)
        }

        // Setup adapter immediately with initials
        adapter = GroupMembersAdapter(isOwner)
        binding.membersList.layoutManager = LinearLayoutManager(requireContext())
        binding.membersList.adapter = adapter
        adapter.submitList(group.participants)

        adapter.removeClickedEvent.observe(viewLifecycleOwner) {
            it.consume { participant ->
                removeMember(domain, token, userAgent, participant)
            }
        }

        // Fetch avatars in background and update one by one
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val repository = LoquaceGroupsRepository()
            for (participant in group.participants) {
                try {
                    val contact = repository.getContactByJid(domain, token, userAgent, participant.account)

                    withContext(Dispatchers.Main) {
                        adapter.updatePresence(participant.account, contact?.presence?.status)
                    }

                    val pictureUrl = contact?.pictureUrl
                    if (pictureUrl != null) {
                        val bytes = LoquaceMediaDownloader.downloadBytes(
                            url    = pictureUrl,
                            token  = token,
                            domain = domain
                        )
                        if (bytes != null) {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                withContext(Dispatchers.Main) {
                                    adapter.updateAvatar(participant.account, bitmap)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                   println("Failed to process participant ${participant.account}: ${e.message}")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val bottomSheet = dialog?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val screenHeight = resources.displayMetrics.heightPixels
                val sheetHeight = (screenHeight * 0.67).toInt()
                it.layoutParams.height = sheetHeight
                it.requestLayout()
                behavior.peekHeight = sheetHeight
                behavior.isFitToContents = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } catch (e: Exception) {
            println(">>> onStart error: ${e.message}")
        }
    }

    private fun removeMember(domain: String, token: String, userAgent: String, participant: GroupParticipant) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.group_remove_member))
            .setMessage(getString(R.string.group_remove_member_confirmation, participant.fullName ?: participant.account))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    val success = withContext(Dispatchers.IO) {
                        LoquaceGroupsRepository().removeParticipant(
                            domain    = domain,
                            token     = token,
                            userAgent = userAgent,
                            groupJid  = group.jid,
                            userJid   = participant.account
                        )
                    }
                    if (success) {
                        val updated = adapter.currentList.filter { it.account != participant.account }
                        adapter.submitList(updated)
                        binding.memberCount.text = "${updated.size} ${getString(R.string.group_members)}"
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmation(domain: String, token: String, userAgent: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.group_delete))
            .setMessage(getString(R.string.group_delete_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    val success = withContext(Dispatchers.IO) {
                        LoquaceGroupsRepository().deleteGroup(
                            domain    = domain,
                            token     = token,
                            userAgent = userAgent,
                            groupJid  = group.jid
                        )
                    }
                    if (success) {
                        Log.i("$TAG Group ${group.jid} deleted")
                        dismiss()
                        onGroupDeleted()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showAddMemberDialog(domain: String, token: String, userAgent: String) {
        val existingJids = group.participants.map { it.account }.toSet()

        val picker = ParticipantPickerBottomSheet(
            title        = getString(R.string.add_participants_title),
            confirmLabel = getString(R.string.add),
            excludedJids = existingJids,
            onConfirm    = { selected ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        for (contact in selected) {
                            val jid = contact.chats?.firstOrNull {
                                it.type == "xmpp"
                            }?.account ?: continue
                            LoquaceGroupsRepository().inviteParticipant(
                                domain    = domain,
                                token     = token,
                                userAgent = userAgent,
                                groupJid  = group.jid,
                                userJid   = jid
                            )
                        }
                    }
                    // Refresh members list
                    val details = withContext(Dispatchers.IO) {
                        LoquaceGroupsRepository().getGroupDetails(domain, token, userAgent, group.jid)
                    }
                    if (details != null) {
                        adapter.submitList(details.participants)
                        binding.memberCount.text = "${details.participants.size} ${getString(R.string.group_members)}"
                    }
                }
            }
        )
        picker.show(parentFragmentManager, "ParticipantPicker")
    }
}