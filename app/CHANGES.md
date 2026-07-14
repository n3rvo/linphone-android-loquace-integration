# Loquace Modifications to Linphone Android

This file documents all modifications made to the original Linphone Android codebase.
When merging from upstream `release/6.2`, recheck and reapply these changes.

---

## `app/src/main/java/org/linphone/LinphoneApplication.kt`

### Change: Initialize LoquaceCoreProvider after Core starts
### Change: App-wide block on Night Mode
### Change: Apply saved language on app start

---

## `app/src/main/java/org/linphone/ui/main/MainActivity.kt`

### Change: Replace first launch welcome screen with Loquace login
### Change: Add session check in onResume
### Change: Add XMPP reconnection on app foreground/background
### Change: Check for DB corruption flag on startup
### Change: Use startService instead of startForegroundService for XmppConnectionService

---

## `app/src/main/java/org/linphone/notifications/NotificationsManager.kt`

### Change: Hide video answer button for incoming audio calls
### Change: Update caller Person icon in incoming call notification

---

## `app/src/main/java/org/linphone/ui/call/viewmodel/CurrentCallViewModel.kt`

### Change: Fetch Loquace contact info in configureCall()
### Change: Force audio-only for incoming calls
### Change: Fix default speaker on incoming calls

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/MainViewModel.kt`

### Change: Fetch Loquace name for call alert topbar

---

## `app/src/main/res/layout/call_active_fragment.xml`
## `app/src/main/res/layout/call_incoming_fragment.xml`
## `app/src/main/res/layout/call_outgoing_fragment.xml`

### Change: White background, updated text/icon colors

---

## `app/src/main/res/xml/network_security_config.xml` *(NEW)*
## `app/src/main/res/raw/mozilla_ca_bundle.pem` *(NEW)*

### Change: Add Mozilla CA bundle for TLS certificate validation

---

## `app/src/main/java/org/linphone/ui/main/contacts/viewmodel/ContactViewModel.kt`

### Change: Add loquacePresence and loquacePresenceLabel LiveData
### Change: Intercept calls through Loquace `/api/v2/calls` endpoint
### Change: Emergency number check before API call interception

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/SharedMainViewModel.kt`

### Change: Add displayedContactPresence field

---

## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactsListFragment.kt`

### Change: Pass presence to SharedMainViewModel on contact click
### Change: Populate presence map when loading contacts
### Change: Fix race condition between contact tabs
### Change: Remove SIP address entry for Loquace contacts in loadMoreContacts()

---

## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactFragment.kt`

### Change: Set loquacePresence and loquacePresenceLabel from SharedMainViewModel

---

## `app/src/main/res/layout/contact_fragment.xml`

### Change: Hide video call and chat buttons permanently
### Change: Update status text to use loquacePresenceLabel with loquacePresenceTextColor adapter

---

## `app/src/main/res/layout/chat_list_fragment.xml`

### Change: Add search bar above tab layout
### Change: Add TabLayout, wrap content in panel, add Create Group FAB

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationsListFragment.kt`

### Change: Add XMPP chat tabs, adapter, conversation navigation and group creation
### Change: Wire up chat search bar
### Change: Replace `showContactPickerForGroup()` with `ParticipantPickerBottomSheet`
### Change: Add conversation long press delete confirmation dialog

---

## `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt`

### Change: Add search support
### Change: Add fullGroupList backing field for unfiltered groups

---

## `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt`

### Change: Call updateConversationDisplayName after resolving display name
### Change: Set group chat avatar to custom icon
### Change: Refresh avatar model after resolving 1-1 contact name via model.update(null)
### Change: Fix chat list timestamp display

---

## `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt`

### Change: Fix early-return bug skipping avatar model posting
### Change: Set group chat avatar to custom icon in conversation header

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppGroupDetailsBottomSheet.kt`

### Change: Replace `showAddMemberDialog()` with `ParticipantPickerBottomSheet`

---

## `app/src/main/java/org/linphone/utils/DataBindingUtils.kt`

### Change: Add loquacePresence binding adapter
### Change: Add loquacePresenceTextColor binding adapter

---

## `app/src/main/res/drawable/presence_ring_online.xml` *(NEW)*
## `app/src/main/res/drawable/presence_ring_away.xml` *(NEW)*
## `app/src/main/res/drawable/presence_ring_busy.xml` *(NEW)*
## `app/src/main/res/drawable/presence_ring_offline.xml` *(NEW)*
## `app/src/main/res/drawable/switch_thumb_tint.xml` *(NEW)*
## `app/src/main/res/drawable/switch_track_tint.xml` *(NEW)*

---

## `app/src/main/res/layout/contact_avatar.xml`

### Change: Add Loquace presence ring

---

## `app/src/main/res/layout/contact_list_cell.xml`

### Change: Add loquacePresence variable and pass to avatar include

---

## `app/src/main/java/org/linphone/ui/main/contacts/adapter/ContactsListAdapter.kt`

### Change: Add presence map and pass to cell binding

---

## `app/src/main/res/navigation/main_nav_graph.xml`

### Change: Add LoquaceDialerFragment, set startDestination to dialer

---

## `app/src/main/res/navigation/chat_nav_graph.xml`

### Change: Add XmppConversationFragment and LoquaceAboutFragment actions

---

## `app/src/main/res/layout/bottom_nav_bar.xml`

### Change: Add Dialer tab between Contacts and Calls

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/AbstractMainViewModel.kt`

### Change: Add dialer navigation support

---

## `app/src/main/java/org/linphone/ui/main/fragment/AbstractMainFragment.kt`

### Change: Add dialer navigation, initViews overload, tab selection state
### Change: Add presence ring update for top bar avatar

---

## `app/src/main/res/layout/history_list_fragment.xml`

### Change: Add TabLayout, wrap in panel, fix empty state, hide FAB

---

## `app/src/main/java/org/linphone/ui/main/history/viewmodel/HistoryListViewModel.kt`

### Change: Add tab state and Loquace call history LiveData

---

## `app/src/main/java/org/linphone/ui/main/history/fragment/HistoryListFragment.kt`

### Change: Replace Linphone call history with Loquace API call history
### Change: Guard against empty token on first install
### Change: Add try-catch to loadMoreCalls

---

## `app/src/main/java/org/linphone/ui/main/history/adapter/HistoryListAdapter.kt`

### Change: Add Loquace call log view type

---

## `app/src/main/java/org/linphone/ui/main/history/model/CallLogModelWrapper.kt`

### Change: Add loquaceCallLogModel field

---

## `app/src/main/res/layout/history_list_cell.xml`

### Change: Add loquaceModel variable and update bindings

---

## `app/src/main/res/layout/main_drawer_menu.xml`

### Change: Replace Linphone drawer with Loquace custom drawer
### Change: Add language accordion row with Spinner
### Change: Apply LoquaceSwitch style with thumb and track tint selectors

---

## `app/src/main/res/values/styles.xml`

### Change: Add LoquaceSwitch style

---

## `app/src/main/java/org/linphone/ui/main/fragment/DrawerMenuFragment.kt`

### Change: Wire up Loquace drawer sections
### Change: Add language accordion toggle and spinner
### Change: Observe `languageChangedEvent` and call `applyLanguage()`

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/LoquaceDrawerMenuViewModel.kt`

### Change: Add `setLanguage()` and `languageChangedEvent`

---

## `app/src/main/res/layout/settings_fragment.xml`
## `app/src/main/res/layout/settings_calls.xml`
## `app/src/main/res/layout/settings_network.xml`

### Change: Hide unwanted settings sections, hide push notifications toggle

---

## `app/src/main/res/layout/settings_advanced_fragment.xml`

### Change: Hide unwanted advanced settings, add permissions section

---

## `app/src/main/java/org/linphone/ui/main/settings/fragment/SettingsAdvancedFragment.kt`

### Change: Add permissions section, remove audio device pickers
### Change: Add CALL_PHONE PermissionItem to setupPermissionsList()

---

## `app/src/main/java/org/linphone/core/CorePreferences.kt`

### Change: Default `disableCallRecordings` to `true`
### Change: Default `keepServiceAlive` to `true`

---

## `app/src/main/java/org/linphone/compatibility/Compatibility.kt`

### Change: Add CALL_PHONE to getAllRequiredPermissionsArray() (pre-API33 branch)

---

## `app/src/main/java/org/linphone/compatibility/Api33Compatibility.kt`

### Change: Add CALL_PHONE to getAllRequiredPermissionsArray()

---

## `app/src/main/java/org/linphone/ui/assistant/viewmodel/PermissionsViewModel.kt`

### Change: Add callPhonePermissionGranted LiveData

---

## `app/src/main/res/layout/assistant_permissions_fragment.xml`

### Change: Add CALL_PHONE permission row to onboarding screen

---

## `app/src/main/java/org/linphone/ui/assistant/fragment/PermissionsFragment.kt`

### Change: Set keep_service_alive based on notification permission grant status
After permissions are granted/skipped, sets `keep_service_alive = true`
if `POST_NOTIFICATIONS` was granted, `false` otherwise, then calls
`coreContext.startKeepAliveService()`.

---

## `app/src/main/java/org/linphone/compatibility/GenericActivity.kt`

### Change: Lock orientation to portrait

---

## `app/src/main/AndroidManifest.xml`

### Change: Disable Auto Backup
### Change: Re-enable predictive back gesture
### Change: Lock app to portrait orientation
### Change: Register XMPP foreground service as specialUse — REMOVED
### Change: Replace Linphone's Firebase service with Loquace's
### Change: Update LoquaceLoginActivity declaration
### Change: Update FileProvider authority
### Change: Remove duplicate FileProvider
### Change: Add network security config
### Change: Add CALL_PHONE permission
### Change: Add localeConfig reference

---

## `app/src/main/res/xml/locales_config.xml`

### Change: Add Italian locale

---

## `app/src/main/res/values/strings.xml`

### Change: Add all new string resources for Loquace features

---

## `app/src/main/res/values-it/strings.xml` *(NEW)*

### Change: Add Italian translations for all strings
Note: `file_provider_loquace` intentionally excluded.

---

## `app/src/main/java/org/linphone/utils/FileUtils.kt`
## `app/src/main/java/org/linphone/ui/fileviewer/MediaViewerActivity.kt`
## `app/src/main/java/org/linphone/ui/fileviewer/FileViewerActivity.kt`
## `app/src/main/java/org/linphone/ui/main/recordings/fragment/RecordingsListFragment.kt`
## `app/src/main/java/org/linphone/ui/main/recordings/fragment/RecordingMediaPlayerFragment.kt`
## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactFragment.kt`
## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactsListFragment.kt`
## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationFragment.kt`
## `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppConversationFragment.kt`

### Change: Update FileProvider authority reference

---

## `app/build.gradle.kts`

### Change: Add Gson dependency and update applicationId

---

## `.gitignore`

### Change: Exclude google-services.json from version control

---

## `assets/linphonerc_default`
## `assets/linphonerc_factory`

### Change: Remove sip.linphone.org references, disable Linphone chat features

---

## New files in `app` module

### `app/src/main/res/drawable/loquace_logo.xml` *(NEW)*
### `app/src/main/res/drawable/loquace_splashscreen.xml` *(NEW)*
### `app/src/main/res/drawable/drawer_background.xml` *(NEW)*
### `app/src/main/res/drawable/ic_permission_granted.xml` *(NEW)*
### `app/src/main/res/drawable/ic_permission_denied.xml` *(NEW)*
### `app/src/main/res/layout/loquace_permission_item.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/settings/LoquacePermissionsAdapter.kt` *(NEW)*
### `app/src/main/res/layout/loquace_dialer_fragment.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/dialer/viewmodel/LoquaceDialerViewModel.kt` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/dialer/fragment/LoquaceDialerFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_list_cell.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_conversation_fragment.xml` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppMessagesAdapter.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_incoming.xml` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_outgoing.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/LoquaceVoiceRecorder.kt` *(NEW)*
### `app/src/main/res/layout/loquace_group_details_bottom_sheet.xml` *(NEW)*
### `app/src/main/res/layout/loquace_group_member_cell.xml` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/GroupMembersAdapter.kt` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppGroupDetailsBottomSheet.kt` *(MODIFIED)*
### `app/src/main/res/layout/loquace_participant_picker_bottom_sheet.xml` *(NEW)*
### `app/src/main/res/layout/loquace_participant_picker_cell.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/fragment/ParticipantPickerBottomSheet.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/viewmodel/LoquaceDrawerMenuViewModel.kt` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/help/fragment/LoquaceAboutFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_about_fragment.xml` *(NEW)*
### `app/src/main/java/org/linphone/core/LoquaceFirebaseMessagingService.kt` *(MODIFIED)*

**Key changes to `LoquaceFirebaseMessagingService`:**
- `onNewToken()` stores token in `loquace_flags` SharedPreferences as
  `pending_fcm_token` for use when core isn't ready yet
- `onMessageReceived()` SIP call-id branch now calls
  `core.defaultAccount?.refreshRegister()` before
  `super.onMessageReceived()` to ensure FreeSWITCH has the current
  contact address before routing the SIP INVITE

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/LoquaceLogoutManager.kt` *(MODIFIED)*
- `network/CallsApi.kt` *(NEW)*
- `utils/EmergencyCallUtils.kt` *(NEW)*
- `utils/GroupIconUtils.kt` *(NEW)*
- `res/drawable/loquace_group_icon.xml` *(NEW)*
- `storage/LoquaceDatabase.kt` *(MODIFIED)*
- `storage/entity/XmppConversationEntity.kt` *(MODIFIED)*
- `storage/SessionManager.kt`
- `sip/LoquaceSipConfigurator.kt` *(MODIFIED)*:
    - Removed G.729 test codec code
    - Added pending FCM token application before `core.addAccount()` to
      ensure first SIP REGISTER contains the correct push token
- `ui/LoquaceLoginActivity.kt` *(MODIFIED)*
- `ui/activity_login.xml`
- `xmpp/LoquaceXmppManager.kt` *(MODIFIED)*:
    - MAM history fetch switched from `result.messages` to
      `result.mamResultExtensions`
- `xmpp/XmppConversation.kt` *(MODIFIED)*
- `xmpp/XmppConnectionService.kt` *(MODIFIED)*:
    - Removed foreground service requirement
    - Changed to regular background service
    - Changed `START_STICKY` to `START_NOT_STICKY`
- `xmpp/XmppMessage.kt`, `xmpp/XmppHttpUploadManager.kt`, `xmpp/AttachmentType.kt`
- `storage/dao/XmppConversationDao.kt` *(MODIFIED)*

**Dependencies added to `loquace-integration`:**
- `retrofit2:retrofit`, `retrofit2:converter-gson`
- `androidx.security:security-crypto`
- `net.zetetic:sqlcipher-android`, `androidx.sqlite:sqlite`
- `androidx.room:room-runtime`, `room-ktx`, `room-compiler`
- `com.google.firebase:firebase-messaging`
- `org.linphone:linphone-sdk-android`
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `com.google.android.material:material:1.11.0`
- `org.igniterealtime.smack:smack-android/tcp/im/extensions/sasl:4.4.8`
  (xpp3 excluded)