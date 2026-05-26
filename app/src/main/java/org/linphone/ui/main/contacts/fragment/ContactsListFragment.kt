/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.main.contacts.fragment

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.linphone.LinphoneApplication.Companion.coreContext
import java.io.File
import org.linphone.R
import org.linphone.core.FriendList
import org.linphone.core.tools.Log
import org.linphone.databinding.ContactsListFilterPopupMenuBinding
import org.linphone.databinding.ContactsListFragmentBinding
import org.linphone.ui.fileviewer.FileViewerActivity
import org.linphone.ui.fileviewer.MediaViewerActivity
import org.linphone.ui.main.MainActivity
import org.linphone.ui.main.contacts.adapter.ContactsListAdapter
import org.linphone.ui.main.contacts.model.ContactAvatarModel
import org.linphone.ui.main.contacts.viewmodel.ContactsListViewModel
import org.linphone.ui.main.fragment.AbstractMainFragment
import org.linphone.utils.ConfirmationDialogModel
import org.linphone.utils.DialogUtils
import org.linphone.utils.Event
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.linphone.core.Factory
import org.linphone.loquace_integration.network.ContactResponse
import org.linphone.loquace_integration.network.LoquaceAvatarHelper
import org.linphone.loquace_integration.network.LoquaceConfig
import org.linphone.loquace_integration.network.LoquaceContactsRepository
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.ui.main.contacts.viewmodel.ContactsListViewModel.ContactTab
import org.linphone.utils.FileUtils

@UiThread
class ContactsListFragment : AbstractMainFragment() {
    companion object {
        private const val TAG = "[Contacts List Fragment]"
    }

    private val contactsRepository = LoquaceContactsRepository()
    private var currentOffset = 0
    private var isLoadingMore = false
    private var hasMoreContacts = true
    private lateinit var domain: String
    private lateinit var token: String
    private lateinit var userAgent: String
    private val contactPresenceMap = mutableMapOf<String, String?>()

    private lateinit var binding: ContactsListFragmentBinding

    private lateinit var listViewModel: ContactsListViewModel

    private lateinit var adapter: ContactsListAdapter
    private lateinit var favouritesAdapter: ContactsListAdapter

    private var bottomSheetDialog: BottomSheetDialogFragment? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("$TAG READ_CONTACTS permission has been granted, start contacts loader")
            (requireActivity() as MainActivity).loadContacts()
        } else {
            Log.w("$TAG READ_CONTACTS permission has been denied")
        }
    }

    private val swipeToRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        Log.i("$TAG Swipe to refresh triggered, updating CardDAV friend lists")
        listViewModel.refreshCardDavContacts()
    }

    override fun onDefaultAccountChanged() {
        Log.i(
            "$TAG Default account changed, updating avatar in top bar & refreshing contacts list"
        )
        listViewModel.applyCurrentDefaultAccountFilter()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (findNavController().currentDestination?.id == R.id.newContactFragment
        ) {
            // Holds fragment in place while new fragment slides over it
            return AnimationUtils.loadAnimation(activity, R.anim.hold)
        }
        return super.onCreateAnimation(transit, enter, nextAnim)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ContactsListAdapter()
        favouritesAdapter = ContactsListAdapter(favourites = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ContactsListFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listViewModel = ViewModelProvider(this)[ContactsListViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = listViewModel
        observeToastEvents(listViewModel)

        // Disabled by default, may be enabled in onResume()
        binding.contactsListSwipeRefresh.isEnabled = false
        binding.contactsListSwipeRefresh.setOnRefreshListener(swipeToRefreshListener)

        binding.contactsList.setHasFixedSize(true)
        binding.contactsList.layoutManager = LinearLayoutManager(requireContext())
        binding.contactsList.outlineProvider = outlineProvider

        binding.favouritesContactsList.setHasFixedSize(true)
        val favouritesLayoutManager = LinearLayoutManager(requireContext())
        favouritesLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.favouritesContactsList.layoutManager = favouritesLayoutManager

        configureAdapter(adapter)
        configureAdapter(favouritesAdapter)

        listViewModel.isListFiltered.observe(viewLifecycleOwner) { filtered ->
            binding.contactsList.clipToOutline = filtered
        }

        listViewModel.contactsList.observe(viewLifecycleOwner) {
            if (listViewModel.currentTab.value != ContactTab.PHONE) {
                Log.d(TAG, "Phone contacts update ignored, current tab is not PHONE")
                return@observe
            }
            listViewModel.isContactsEmpty.value = it.isEmpty()
            adapter.submitList(it)
            if (binding.contactsList.adapter != adapter) {
                binding.contactsList.adapter = adapter
            }
            Log.i("$TAG Contacts list updated with [${it.size}] items")
            listViewModel.fetchInProgress.value = false
        }

        listViewModel.searchFilter.observe(viewLifecycleOwner) { query ->
            when (listViewModel.currentTab.value) {
                ContactTab.PBX  -> resetAndLoadContacts(LoquaceContactsRepository.TYPE_PBX, query)
                ContactTab.USER -> resetAndLoadContacts(LoquaceContactsRepository.TYPE_USER, query)
                else -> { /* handled by existing MagicSearch */ }
            }
        }

        listViewModel.favouritesList.observe(
            viewLifecycleOwner
        ) {
            favouritesAdapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.favouritesContactsList.adapter != favouritesAdapter) {
                binding.favouritesContactsList.adapter = favouritesAdapter
            }

            Log.i("$TAG Favourites contacts list updated with [${it.size}] items")
        }

        listViewModel.vCardTerminatedEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val contactName = pair.first
                val file = pair.second
                Log.i(
                    "$TAG Friend [$contactName] was exported as vCard file [${file.absolutePath}], sharing it"
                )
                shareContact(contactName, file)
            }
        }

        listViewModel.cardDavSynchronizationCompletedEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG CardDAV synchronization has completed")
                binding.contactsListSwipeRefresh.isRefreshing = false
            }
        }

        binding.setOnNewContactClicked {
            sharedViewModel.showNewContactEvent.value = Event(true)
        }

        binding.setFilterClickListener {
            showFilterPopupMenu(binding.topBar.extraAction)
        }

        sharedViewModel.showContactEvent.observe(viewLifecycleOwner) {
            it.consume { refKey ->
                Log.i("$TAG Displaying contact with ref key [$refKey]")
                val navController = binding.contactsNavContainer.findNavController()
                val action = ContactFragmentDirections.actionGlobalContactFragment(
                    refKey
                )
                navController.navigate(action)
            }
        }

        sharedViewModel.showNewContactEvent.observe(viewLifecycleOwner) {
            it.consume {
                if (findNavController().currentDestination?.id == R.id.contactsListFragment) {
                    Log.i("$TAG Opening contact editor for creating new contact")
                    val action =
                        ContactsListFragmentDirections.actionContactsListFragmentToNewContactFragment()
                    findNavController().navigate(action)
                }
            }
        }

        sharedViewModel.forceRefreshContactsList.observe(viewLifecycleOwner) {
            it.consume {
                listViewModel.filter()
            }
        }

        sharedViewModel.displayFileEvent.observe(viewLifecycleOwner) {
            it.consume { bundle ->
                if (findNavController().currentDestination?.id == R.id.contactsListFragment) {
                    val path = bundle.getString("path", "")
                    val isMedia = bundle.getBoolean("isMedia", false)
                    if (path.isEmpty()) {
                        Log.e("$TAG Can't navigate to file viewer for empty path!")
                        return@consume
                    }

                    Log.i(
                        "$TAG Navigating to [${if (isMedia) "media" else "file"}] viewer fragment with path [$path]"
                    )
                    if (isMedia) {
                        val intent = Intent(requireActivity(), MediaViewerActivity::class.java)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    } else {
                        val intent = Intent(requireActivity(), FileViewerActivity::class.java)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }
                }
            }
        }

        // AbstractMainFragment related

        listViewModel.title.value = getString(R.string.bottom_navigation_contacts_label)
        setViewModel(listViewModel)
        initViews(
            binding.slidingPaneLayout,
            binding.topBar,
            binding.bottomNavBar,
            R.id.contactsListFragment
        )

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("$TAG READ_CONTACTS permission wasn't granted yet, asking for it now")
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        // Load credentials
        val sessionManager = SessionManager(requireContext())
        domain = sessionManager.getDomain() ?: ""
        token = sessionManager.getToken() ?: ""
        userAgent = buildUserAgent(requireContext())

        // Setup tabs
        val tabLayout = binding.contactsTabLayout ?: return
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.contacts_tab_phone)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.contacts_tab_pbx)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.contacts_tab_user)))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> listViewModel.switchTab(ContactTab.PHONE)
                    1 -> {
                        listViewModel.switchTab(ContactTab.PBX)
                        resetAndLoadContacts(LoquaceContactsRepository.TYPE_PBX)
                    }
                    2 -> {
                        listViewModel.switchTab(ContactTab.USER)
                        resetAndLoadContacts(LoquaceContactsRepository.TYPE_USER)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Infinite scroll
        binding.contactsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!hasMoreContacts || isLoadingMore) return
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = layoutManager.itemCount
                if (lastVisible >= total - 5) {
                    val currentType = when (listViewModel.currentTab.value) {
                        ContactTab.PBX  -> LoquaceContactsRepository.TYPE_PBX
                        ContactTab.USER -> LoquaceContactsRepository.TYPE_USER
                        else            -> return
                    }
                    loadMoreContacts(currentType)
                }
            }
        })

        // Observe Loquace contacts
        listViewModel.loquaceContactsList.observe(viewLifecycleOwner) {
            val tab = listViewModel.currentTab.value
            if (tab != ContactTab.PBX && tab != ContactTab.USER) {
                Log.d(TAG, "Loquace contacts update ignored, current tab is PHONE")
                return@observe
            }
            listViewModel.isContactsEmpty.value = it.isEmpty()
            adapter.submitList(it)
            if (binding.contactsList.adapter != adapter) {
                binding.contactsList.adapter = adapter
            }
            Log.i("$TAG Loquace contacts list updated with [${it.size}] items")
        }

    }

    override fun onPause() {
        super.onPause()

        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }

    override fun onResume() {
        super.onResume()

        coreContext.postOnCoreThread { core ->
            val cardDavFriendList = core.friendsLists.find {
                it.type == FriendList.Type.CardDAV
            }
            val cardDavFriendListFound = cardDavFriendList != null
            if (cardDavFriendListFound) {
                Log.i("$TAG CardDAV friend list [${cardDavFriendList.displayName}] found, enabling swipe to refresh")
            } else {
                Log.i("$TAG No CardDAV friend list was found, disabling swipe to refresh")
            }
            coreContext.postOnMainThread {
                binding.contactsListSwipeRefresh.isEnabled = cardDavFriendListFound
            }
        }

        // Force contacts load if permission is granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            (requireActivity() as MainActivity).loadContacts()
        }
    }

    private fun configureAdapter(adapter: ContactsListAdapter) {
        adapter.contactLongClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                val modalBottomSheet = ContactsListMenuDialogFragment(
                    model.isFavourite.value == true,
                    model.isStored,
                    isReadOnly = model.isReadOnly,
                    isNative = model.isNative,
                    { // onDismiss
                        adapter.resetSelection()
                    },
                    { // onFavourite
                        listViewModel.toggleContactFavoriteFlag(model)
                    },
                    { // onShare
                        Log.i(
                            "$TAG Sharing friend [${model.name.value}], exporting it as vCard file first"
                        )
                        listViewModel.exportContactAsVCard(model.friend)
                    },
                    { // onDelete
                        showDeleteConfirmationDialog(model)
                    }
                )
                modalBottomSheet.show(parentFragmentManager, ContactsListMenuDialogFragment.TAG)
                bottomSheetDialog = modalBottomSheet
            }
        }

        adapter.contactClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                sharedViewModel.displayedFriend = model.friend
                sharedViewModel.showContactEvent.value = Event(model.id)
            }
        }
    }

    private fun shareContact(name: String, file: File) {
        val publicUri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().getString(R.string.file_provider_loquace),
            file
        )
        Log.i("$TAG Public URI for vCard file is [$publicUri], starting intent chooser")

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, publicUri)
            putExtra(Intent.EXTRA_SUBJECT, name)
            type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        try {
            startActivity(shareIntent)
        } catch (anfe: ActivityNotFoundException) {
            Log.e("$TAG Failed to start intent chooser: $anfe")
        }
    }

    private fun showFilterPopupMenu(view: View) {
        val popupView: ContactsListFilterPopupMenuBinding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.contacts_list_filter_popup_menu,
            null,
            false
        )
        popupView.seeAllSelected = listViewModel.areAllContactsDisplayed.value == true
        popupView.showLinphoneFilter = listViewModel.isDefaultAccountLinphone.value == true

        val popupWindow = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.setNoFilterClickListener {
            if (listViewModel.areAllContactsDisplayed.value != true) {
                listViewModel.changeContactsFilter(
                    onlyLinphoneContacts = false,
                    onlySipContacts = false
                )
            }
            popupWindow.dismiss()
        }

        popupView.setLinphoneOnlyClickListener {
            if (listViewModel.areAllContactsDisplayed.value == true) {
                listViewModel.changeContactsFilter(
                    onlyLinphoneContacts = true,
                    onlySipContacts = false
                )
            }
            popupWindow.dismiss()
        }

        popupView.setSipOnlyClickListener {
            if (listViewModel.areAllContactsDisplayed.value == true) {
                listViewModel.changeContactsFilter(
                    onlyLinphoneContacts = false,
                    onlySipContacts = true
                )
            }
            popupWindow.dismiss()
        }

        // Elevation is for showing a shadow around the popup
        popupWindow.elevation = 20f
        popupWindow.showAsDropDown(view, 0, 0, Gravity.BOTTOM)
    }

    private fun showDeleteConfirmationDialog(contactModel: ContactAvatarModel) {
        val model = ConfirmationDialogModel()
        val dialog = DialogUtils.getDeleteContactConfirmationDialog(
            requireActivity(),
            model,
            contactModel.contactName.orEmpty()
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                listViewModel.deleteContact(contactModel)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun resetAndLoadContacts(type: String, query: String = "") {
        /*if (query.isEmpty()) {
            val cache = when (type) {
                LoquaceContactsRepository.TYPE_PBX -> listViewModel.pbxContactsCache.value
                LoquaceContactsRepository.TYPE_USER -> listViewModel.userContactsCache.value
                else -> null
            }
            if (!cache.isNullOrEmpty()) {
                listViewModel.loquaceContactsList.value = cache
                // Restore pagination state
                currentOffset = when (type) {
                    LoquaceContactsRepository.TYPE_PBX -> listViewModel.pbxOffset
                    else -> listViewModel.userOffset
                }
                hasMoreContacts = when (type) {
                    LoquaceContactsRepository.TYPE_PBX -> listViewModel.pbxHasMore
                    else -> listViewModel.userHasMore
                }
                return
            }
        }*/
        currentOffset = 0
        hasMoreContacts = true
        listViewModel.loquaceContactsList.value = arrayListOf()
        loadMoreContacts(type, query)
    }

    private fun loadMoreContacts(type: String, query: String = "") {
        if (isLoadingMore || !hasMoreContacts) return
        isLoadingMore = true
        listViewModel.fetchInProgress.value = true

        // Capture the tab at call time
        val calledForTab = listViewModel.currentTab.value

        viewLifecycleOwner.lifecycleScope.launch {
            val contacts = contactsRepository.fetchContacts(
                domain    = domain,
                token     = token,
                userAgent = userAgent,
                type      = type,
                offset    = currentOffset,
                query     = query
            )

            if (contacts.isEmpty() || contacts.size < LoquaceConfig.CONTACTS_PAGE_SIZE) {
                hasMoreContacts = false
            }

            val avatarPaths = mutableMapOf<String, String>()
            for (contact in contacts) {
                val path = fetchAndSaveAvatar(contact, requireContext().filesDir)
                if (path != null) avatarPaths[contact.id] = path
            }

            val friends = arrayListOf<ContactAvatarModel>()
            coreContext.postOnCoreThread { core ->
                for (contact in contacts) {
                    val friend = core.createFriend()
                    friend.name = contact.fullName?.ifEmpty {
                        "${contact.firstName} ${contact.lastName}".trim()
                    } ?: "${contact.firstName} ${contact.lastName}".trim()
                    friend.refKey = contact.id

                    if (!contact.account.isNullOrEmpty()) {
                        val address = Factory.instance().createAddress("sip:${contact.account}")
                        if (address != null) friend.addAddress(address)
                    }

                    contact.phones?.forEach { phone ->
                        friend.addPhoneNumber(phone.number)
                    }

                    avatarPaths[contact.id]?.let {
                        friend.photo = FileUtils.getProperFilePath(it)
                    }

                    val model = coreContext.contactsManager.getContactAvatarModelForFriend(friend)
                    friends.add(model)
                    adapter.presenceMap[contact.id] = contact.presence?.status
                }

                coreContext.postOnMainThread {
                    // Only post results if we're still on the same tab
                    if (listViewModel.currentTab.value != calledForTab) {
                        Log.d(TAG, "Tab changed during fetch, discarding results for $type")
                        isLoadingMore = false
                        listViewModel.fetchInProgress.value = false
                        return@postOnMainThread
                    }

                    val existing = listViewModel.loquaceContactsList.value ?: arrayListOf()
                    val newList = arrayListOf<ContactAvatarModel>()
                    newList.addAll(existing)
                    newList.addAll(friends)
                    listViewModel.loquaceContactsList.value = newList

                    currentOffset += contacts.size
                    isLoadingMore = false
                    listViewModel.fetchInProgress.value = false
                }
            }
        }
    }

    private fun buildUserAgent(context: Context): String {
        val appName = context.getString(org.linphone.loquace_integration.R.string.app_name_agent)
        val osVersion = Build.VERSION.RELEASE
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val deviceModel = Build.MODEL
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return "$appName/Android-$osVersion/$versionName/$deviceModel/$androidId"
    }

    private suspend fun fetchAndSaveAvatar(
        contact: ContactResponse,
        filesDir: File
    ): String? {
        return LoquaceAvatarHelper.fetchAndSaveAvatar(
            contactId  = contact.id,
            pictureUrl = contact.pictureUrl,
            domain     = domain,
            token      = token,
            userAgent  = userAgent,
            filesDir   = filesDir
        )
    }
}
