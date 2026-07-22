package org.linphone.ui.call.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.Address
import org.linphone.core.Factory
import org.linphone.databinding.LoquaceCallContactPickerBinding
import org.linphone.loquace_integration.network.CallContactRequest
import org.linphone.loquace_integration.network.CallRequest
import org.linphone.loquace_integration.network.ContactResponse
import org.linphone.loquace_integration.network.LoquaceAvatarHelper
import org.linphone.loquace_integration.network.LoquaceConfig
import org.linphone.loquace_integration.network.LoquaceContactsRepository
import org.linphone.loquace_integration.network.RetrofitClient
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.ui.main.contacts.adapter.ContactsListAdapter
import org.linphone.ui.main.contacts.model.ContactAvatarModel
import org.linphone.ui.main.contacts.viewmodel.ContactsListViewModel
import org.linphone.utils.FileUtils
import org.linphone.utils.LinphoneUtils

class LoquaceCallContactPickerBottomSheet(
    private val skipApiCall: Boolean = false,
    private val onContactSelected: (Address) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "[Call Contact Picker]"
        private const val TAB_DEVICE = 0
        private const val TAB_PBX = 1
        private const val TAB_USER = 2
    }

    private lateinit var binding: LoquaceCallContactPickerBinding
    private lateinit var adapter: ContactsListAdapter

    private lateinit var domain: String
    private lateinit var token: String
    private lateinit var userAgent: String

    private var currentTab = TAB_DEVICE
    private var currentOffset = 0
    private var isLoadingMore = false
    private var hasMoreContacts = true
    private var searchJob: Job? = null
    private var currentQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LoquaceCallContactPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        domain = sessionManager.getDomain() ?: ""
        token = sessionManager.getToken() ?: ""
        userAgent = sessionManager.getUserAgent()

        adapter = ContactsListAdapter()

        adapter.contactClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                handleContactSelected(model)
            }
        }

        binding.contactsList.layoutManager = LinearLayoutManager(requireContext())
        binding.contactsList.adapter = adapter

        // Infinite scroll for Loquace tabs
        binding.contactsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (currentTab == TAB_DEVICE || !hasMoreContacts || isLoadingMore) return
                val lm = recyclerView.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= lm.itemCount - 5) {
                    val type = if (currentTab == TAB_PBX) LoquaceContactsRepository.TYPE_PBX
                    else LoquaceContactsRepository.TYPE_USER
                    loadMoreContacts(type, currentQuery)
                }
            }
        })

        // Tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.contacts_tab_phone)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.contacts_tab_pbx)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.contacts_tab_user)))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentQuery = binding.searchInput.text?.toString().orEmpty()
                when (tab?.position) {
                    TAB_DEVICE -> {
                        currentTab = TAB_DEVICE
                        loadDeviceContacts(currentQuery)
                    }
                    TAB_PBX -> {
                        currentTab = TAB_PBX
                        resetAndLoad(LoquaceContactsRepository.TYPE_PBX, currentQuery)
                    }
                    TAB_USER -> {
                        currentTab = TAB_USER
                        resetAndLoad(LoquaceContactsRepository.TYPE_USER, currentQuery)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Search
        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString().orEmpty()
                currentQuery = query
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    when (currentTab) {
                        TAB_DEVICE -> loadDeviceContacts(query)
                        TAB_PBX -> resetAndLoad(LoquaceContactsRepository.TYPE_PBX, query)
                        TAB_USER -> resetAndLoad(LoquaceContactsRepository.TYPE_USER, query)
                    }
                }
            }
        })

        // Initial load
        loadDeviceContacts("")
    }

    override fun onStart() {
        super.onStart()
        try {
            val bottomSheet = dialog?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val height = (resources.displayMetrics.heightPixels * 0.85).toInt()
                it.layoutParams.height = height
                it.requestLayout()
                behavior.peekHeight = height
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } catch (e: Exception) { }
    }

    private var contactsListViewModel: ContactsListViewModel? = null

    private fun loadDeviceContacts(query: String) {
        if (contactsListViewModel == null) {
            contactsListViewModel = ViewModelProvider(requireActivity())[ContactsListViewModel::class.java]
            contactsListViewModel?.contactsList?.observe(viewLifecycleOwner) { contacts ->
                if (currentTab == TAB_DEVICE) {
                    adapter.submitList(contacts)
                }
            }
        }
        contactsListViewModel?.searchFilter?.value = query
    }

    private fun resetAndLoad(type: String, query: String) {
        currentOffset = 0
        hasMoreContacts = true
        adapter.submitList(emptyList())
        loadMoreContacts(type, query)
    }

    private fun loadMoreContacts(type: String, query: String) {
        if (isLoadingMore || !hasMoreContacts) return
        isLoadingMore = true

        coreContext.postOnMainThread { binding.fetchInProgress.visibility = View.VISIBLE }

        val calledForTab = currentTab

        viewLifecycleOwner.lifecycleScope.launch {
            val contacts = withContext(Dispatchers.IO) {
                try {
                    LoquaceContactsRepository().fetchContacts(
                        domain = domain,
                        token = token,
                        userAgent = userAgent,
                        type = type,
                        offset = currentOffset,
                        query = query
                    )
                } catch (e: Exception) { emptyList() }
            }

            if (currentTab != calledForTab) {
                isLoadingMore = false
                return@launch
            }

            if (contacts.isEmpty() || contacts.size < LoquaceConfig.CONTACTS_PAGE_SIZE) {
                hasMoreContacts = false
            }

            val avatarPaths = withContext(Dispatchers.IO) {
                val map = mutableMapOf<String, String>()
                for (contact in contacts) {
                    val path = LoquaceAvatarHelper.fetchAndSaveAvatar(
                        contactId = contact.id,
                        pictureUrl = contact.pictureUrl,
                        domain = domain,
                        token = token,
                        userAgent = userAgent,
                        filesDir = requireContext().filesDir
                    )
                    if (path != null) map[contact.id] = path
                }
                map
            }

            val models = arrayListOf<ContactAvatarModel>()
            coreContext.postOnCoreThread { core ->
                for (contact in contacts) {
                    val friend = core.createFriend()
                    friend.name = contact.fullName?.ifEmpty {
                        "${contact.firstName} ${contact.lastName}".trim()
                    } ?: "${contact.firstName} ${contact.lastName}".trim()
                    friend.refKey = contact.id

                    contact.phones?.forEach { phone ->
                        friend.addPhoneNumber(phone.number)
                    }

                    avatarPaths[contact.id]?.let {
                        friend.photo = FileUtils.getProperFilePath(it)
                    }

                    val model = coreContext.contactsManager.getContactAvatarModelForFriend(friend)
                    adapter.presenceMap[contact.id] = contact.presence?.status
                    models.add(model)
                }

                coreContext.postOnMainThread {
                    val existing = adapter.currentList.toMutableList()
                    existing.addAll(models)
                    adapter.submitList(existing)
                    currentOffset += contacts.size
                    isLoadingMore = false
                }
            }
        }

        binding.fetchInProgress.visibility = View.GONE
    }

    private fun handleContactSelected(model: ContactAvatarModel) {
        val friend = model.friend
        val number = friend.phoneNumbers.firstOrNull() ?: return

        if (skipApiCall) {
            // For transfer — just build SIP address directly
            coreContext.postOnCoreThread { core ->
                val address = core.interpretUrl(number, LinphoneUtils.applyInternationalPrefix())
                if (address != null) {
                    coreContext.postOnMainThread {
                        onContactSelected(address)
                        dismiss()
                    }
                }
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val contactRequest = when (currentTab) {
                    TAB_USER -> CallContactRequest(
                        id = friend.refKey,
                        number = number
                    )
                    TAB_DEVICE -> CallContactRequest(
                        name = friend.name,
                        number = number
                    )
                    else -> CallContactRequest(
                        id = friend.refKey,
                        number = number
                    )
                }

                val api = RetrofitClient.createCallsApi(domain)
                val response = api.placeCall(token, userAgent, domain, CallRequest(contact = contactRequest))

                if (response.failed) return@launch

                val finalNumber = response.contact.number
                coreContext.postOnCoreThread { core ->
                    val address = core.interpretUrl(finalNumber, LinphoneUtils.applyInternationalPrefix())
                    if (address != null) {
                        coreContext.postOnMainThread {
                            onContactSelected(address)
                            dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to place call through API: ${e.message}")
            }
        }
    }
}