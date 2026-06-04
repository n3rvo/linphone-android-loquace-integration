package org.linphone.ui.main.chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.graphics.Outline
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.R
import org.linphone.databinding.LoquaceParticipantPickerBottomSheetBinding
import org.linphone.databinding.LoquaceParticipantPickerCellBinding
import org.linphone.loquace_integration.network.ContactResponse
import org.linphone.loquace_integration.network.LoquaceAvatarHelper
import org.linphone.loquace_integration.network.LoquaceConfig
import org.linphone.loquace_integration.network.LoquaceGroupsRepository
import org.linphone.loquace_integration.network.LoquaceMediaDownloader
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.utils.setLoquacePresenceRing

class ParticipantPickerBottomSheet(
    private val title: String,
    private val confirmLabel: String,
    private val excludedJids: Set<String> = emptySet(),
    private val onConfirm: (List<ContactResponse>) -> Unit
) : BottomSheetDialogFragment() {

    private class DiffCallback : DiffUtil.ItemCallback<ContactResponse>() {
        override fun areItemsTheSame(old: ContactResponse, new: ContactResponse) =
            old.id == new.id
        override fun areContentsTheSame(old: ContactResponse, new: ContactResponse) =
            old == new
    }

    private lateinit var binding: LoquaceParticipantPickerBottomSheetBinding
    private lateinit var pickerAdapter: ParticipantPickerAdapter

    private lateinit var domain: String
    private lateinit var token: String
    private lateinit var userAgent: String

    private var searchJob: Job? = null
    private var currentOffset = 0
    private var hasMore = true
    private var isLoading = false
    private var currentQuery = ""

    private val selectedContacts = mutableMapOf<String, ContactResponse>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.loquace_participant_picker_bottom_sheet,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        domain = sessionManager.getDomain() ?: ""
        token = sessionManager.getToken() ?: ""
        userAgent = sessionManager.getUserAgent()

        binding.title.text = title
        binding.confirmButton.text = confirmLabel

        pickerAdapter = ParticipantPickerAdapter()
        binding.participantsList.layoutManager = LinearLayoutManager(requireContext())
        binding.participantsList.adapter = pickerAdapter

        // Infinite scroll
        binding.participantsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!hasMore || isLoading) return
                val lm = recyclerView.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= lm.itemCount - 5) {
                    loadMore(currentQuery)
                }
            }
        })

        // Search
        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    resetAndLoad(s?.toString().orEmpty())
                }
            }
        })

        binding.confirmButton.setOnClickListener {
            onConfirm(pickerAdapter.getSelected())
            dismiss()
        }

        // Initial load
        resetAndLoad("")
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
                val sheetHeight = (screenHeight * 0.85).toInt()
                it.layoutParams.height = sheetHeight
                it.requestLayout()
                behavior.peekHeight = sheetHeight
                behavior.isFitToContents = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun resetAndLoad(query: String) {
        currentQuery = query
        currentOffset = 0
        hasMore = true
        pickerAdapter.resetContacts()
        loadMore(query)
    }

    private fun loadMore(query: String) {
        if (isLoading || !hasMore) return
        isLoading = true
        binding.fetchInProgress.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val page = withContext(Dispatchers.IO) {
                try {
                    LoquaceGroupsRepository().fetchChatEnabledContacts(
                        domain    = domain,
                        token     = token,
                        userAgent = userAgent,
                        offset    = currentOffset,
                        query     = query
                    )
                } catch (e: Exception) { emptyList() }
            }

            // Filter excluded JIDs
            val filtered = page.filter { contact ->
                val jid = contact.chats?.firstOrNull { it.type == "xmpp" }?.account
                jid != null && !excludedJids.contains(jid)
            }

            pickerAdapter.appendContacts(filtered)

            if (page.size < LoquaceConfig.CONTACTS_PAGE_SIZE) hasMore = false
            currentOffset += page.size
            isLoading = false
            binding.fetchInProgress.visibility = View.GONE

            // Fetch avatars and presence in background
            for (contact in filtered) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val bytes = contact.pictureUrl?.let {
                        LoquaceMediaDownloader.downloadBytes(it, token, domain)
                    }
                    val bitmap = bytes?.let {
                        android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)
                    }
                    withContext(Dispatchers.Main) {
                        pickerAdapter.updateAvatarAndPresence(
                            contact.id,
                            bitmap,
                            contact.presence?.status
                        )
                    }
                }
            }
        }
    }

    // Inner adapter
    inner class ParticipantPickerAdapter :
        ListAdapter<ContactResponse, ParticipantPickerAdapter.ViewHolder>(DiffCallback()) {

        private val selectedIds = mutableSetOf<String>()
        private val avatars = mutableMapOf<String, android.graphics.Bitmap>()
        private val presences = mutableMapOf<String, String?>()
        private val allContacts = mutableListOf<ContactResponse>()

        fun resetContacts() {
            allContacts.clear()
            submitList(emptyList())
        }

        fun appendContacts(contacts: List<ContactResponse>) {
            allContacts.addAll(contacts)
            submitList(allContacts.toList())
        }

        fun updateAvatarAndPresence(id: String, bitmap: android.graphics.Bitmap?, status: String?) {
            if (bitmap != null) avatars[id] = bitmap
            presences[id] = status
            val pos = currentList.indexOfFirst { it.id == id }
            if (pos != -1) notifyItemChanged(pos)
        }

        // Update getSelected():
        fun getSelected(): List<ContactResponse> {
            return selectedContacts.values.toList()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding: LoquaceParticipantPickerCellBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.loquace_participant_picker_cell,
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(val binding: LoquaceParticipantPickerCellBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(contact: ContactResponse) {
                val name = contact.fullName
                    ?: "${contact.firstName} ${contact.lastName}".trim()

                binding.contactName.text = name
                binding.isSelected = selectedIds.contains(contact.id)

                // Avatar
                binding.avatarContainer.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                binding.avatarContainer.clipToOutline = true

                val bitmap = avatars[contact.id]
                if (bitmap != null) {
                    binding.avatar.setImageBitmap(bitmap)
                    binding.avatar.visibility = View.VISIBLE
                    binding.avatarInitials.visibility = View.GONE
                } else {
                    val initials = name.split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("")
                        .uppercase()
                    binding.avatarInitials.text = initials
                    binding.avatarInitials.visibility = View.VISIBLE
                    binding.avatar.visibility = View.GONE
                }

                // Presence ring
                binding.presenceRing.setLoquacePresenceRing(presences[contact.id])

                // Update selection toggle in bind():
                binding.root.setOnClickListener {
                    if (selectedIds.contains(contact.id)) {
                        selectedIds.remove(contact.id)
                        selectedContacts.remove(contact.id)
                    } else {
                        selectedIds.add(contact.id)
                        selectedContacts[contact.id] = contact
                    }
                    notifyItemChanged(bindingAdapterPosition)
                }

                binding.executePendingBindings()
            }
        }
    }
}