# Loquace Modifications to Linphone Android

This file documents all modifications made to the original Linphone Android codebase.
When merging from upstream `release/6.2`, recheck and reapply these changes.

---

## `app/src/main/java/org/linphone/LinphoneApplication.kt`

### Change: Initialize LoquaceCoreProvider after Core starts
### Change: App-wide block on Night Mode

---

## `app/src/main/java/org/linphone/ui/main/MainActivity.kt`

### Change: Replace first launch welcome screen with Loquace login
### Change: Add session check in onResume
### Change: Add XMPP reconnection on app foreground/background
### Change: Check for DB corruption flag on startup

---

## `app/src/main/java/org/linphone/notifications/NotificationsManager.kt`

### Change: Hide video answer button for incoming audio calls
### Change: Update caller Person icon in incoming call notification
Replaced avatar with initials via `AvatarGenerator` using the Loquace
resolved name, keeping the phone small icon on top clean.

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

### Change: Add loquacePresence LiveData

### Change: Intercept calls through Loquace `/api/v2/calls` endpoint
Added `placeCallThroughApi(address: Address)` private helper:
- Distinguishes SIP contacts (Loquace, `friend.nativeUri` empty) from
  native device contacts (`friend.nativeUri` set) via `isNative` check
- SIP contacts → `CallContactRequest(id = friend.refKey, number = number)`
- Device contacts → `CallContactRequest(name = friend.name, number = number)`
- Posts to `/api/v2/calls`, then builds final SIP address from
  `response.contact.number` via `core.interpretUrl()` before calling
  `coreContext.startAudioCall()`
- `startAudioCall()` and the `listener.onClicked()` `START_AUDIO_CALL`
  branch both updated to call `placeCallThroughApi()` instead of
  `coreContext.startAudioCall()` directly
- Video calls unaffected, still call `coreContext.startVideoCall()` directly

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/SharedMainViewModel.kt`

### Change: Add displayedContactPresence field

---

## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactsListFragment.kt`

### Change: Pass presence to SharedMainViewModel on contact click
### Change: Populate presence map when loading contacts
### Change: Fix race condition between contact tabs
### Change: Remove SIP address entry for Loquace contacts in loadMoreContacts()
Removed `friend.addAddress()` call building `sip:{contact.account}` —
only `friend.addPhoneNumber(phone.number)` is kept. This was creating a
duplicate, non-callable "SIP address" entry alongside the correct
callable phone number entry in the Contact Fragment's numbers list.
XMPP JID info remains separately handled in
`XmppConversationsListViewModel.loadContacts()` via `contact.chats`.

---

## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactFragment.kt`

### Change: Set loquacePresence from SharedMainViewModel

---

## `app/src/main/res/layout/contact_fragment.xml`

### Change: Hide video call and chat buttons permanently
### Change: Update status text to use Loquace presence with color binding adapter

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

---

## `app/src/main/java/org/linphone/ui/main/fragment/DrawerMenuFragment.kt`

### Change: Wire up Loquace drawer sections

---

## `app/src/main/res/layout/settings_fragment.xml`
## `app/src/main/res/layout/settings_calls.xml`
## `app/src/main/res/layout/settings_network.xml`
## `app/src/main/res/layout/settings_advanced_fragment.xml`

### Change: Hide unwanted settings sections, hide push notifications toggle

---

## `app/src/main/java/org/linphone/ui/main/settings/fragment/SettingsAdvancedFragment.kt`

### Change: Add permissions section, remove audio device pickers

---

## `app/src/main/java/org/linphone/core/CorePreferences.kt`

### Change: Default `disableCallRecordings` to `true`

---

## `app/src/main/AndroidManifest.xml`

### Change: Disable Auto Backup
### Change: Re-enable predictive back gesture
### Change: Lock app to portrait orientation
### Change: Register XMPP foreground service as specialUse
### Change: Replace Linphone's Firebase service with Loquace's
### Change: Update LoquaceLoginActivity declaration
### Change: Update FileProvider authority
### Change: Remove duplicate FileProvider
### Change: Add network security config

---

## `app/src/main/res/values/strings.xml`

### Change: Add all new string resources for Loquace features

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

### Change: Intercept calls through Loquace `/api/v2/calls` endpoint
`onCallClicked()` now POSTs `{"contact":{"number": number}}`, builds
final SIP address from `response.contact.number` via
`core.interpretUrl()` before calling `coreContext.startAudioCall()`.

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
### `app/src/main/java/org/linphone/ui/main/viewmodel/LoquaceDrawerMenuViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/help/fragment/LoquaceAboutFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_about_fragment.xml` *(NEW)*
### `app/src/main/java/org/linphone/core/LoquaceFirebaseMessagingService.kt` *(NEW)*

---

## Pending features (publication checklist)
1. ~~Remove video call and chat button from SIP contact card + add status~~ ✅
2. ~~Search implementation for chat contacts and open conversations~~ ✅
3. ~~Improve group chat participant picker (searchable + avatars + status borders)~~ ✅
4. ~~Restyle of in-call screen~~ ✅
5. ~~Conversation long press → delete conversation~~ ✅
6. Video upload optimization
7. In-app media viewer
8. Adding special rules for phone contacts calls and emergency calls *(in progress)*
9. Fix horizontal layout / remove landscape mode
10. Implement translation / language selection
11. Handle call conference UI/logic
12. ~~Add avatar to incoming call notification~~ ✅
13. ~~Fix default speaker on incoming calls~~ ✅
14. Test audio codec g729 support
15. Set group chat avatars to custom icon

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/LoquaceLogoutManager.kt` *(MODIFIED)* — full logout flow
- `network/CallsApi.kt` *(NEW)*:
    - `CallsApi` interface with `placeCall()` POST to `LoquaceConfig.ENDPOINT_PLACE_CALL`
    - `CallRequest`, `CallContactRequest` (id, name, number — all but number optional)
    - `CallResponse`, `CallContactResponse` (failed, account, type, contact)
- `storage/LoquaceDatabase.kt` *(MODIFIED)*:
    - Version bumped to 5
    - `MIGRATION_4_5` adds `displayName` column to `xmpp_conversations`
    - DB validity check, `needs_relogin` flag on corruption
- `storage/entity/XmppConversationEntity.kt` *(MODIFIED)*:
    - Added `displayName: String?` field
- `storage/SessionManager.kt` — `clearSession()` clears all stored data
- `sip/LoquaceSipConfigurator.kt` — push notifications always enabled
- `ui/LoquaceLoginActivity.kt` *(MODIFIED)* — `OnBackPressedCallback`
  calls `moveTaskToBack(true)`
- `ui/activity_login.xml` — redesigned login screen
- `xmpp/LoquaceXmppManager.kt` *(MODIFIED)*:
    - `updateConversationDisplayName()` persists resolved name to StateFlow and DB
    - `loadConversations()` loads `displayName` from DB entity
    - `logout()` clears all XMPP state
    - `deleteConversation()` removes from StateFlow, messages map and DB
- `xmpp/XmppConversation.kt` *(MODIFIED)*:
    - Added `displayName: String? = null` field
- `xmpp/XmppConnectionService.kt` — `specialUse` foreground service type
- `xmpp/XmppMessage.kt`, `xmpp/XmppHttpUploadManager.kt`,
  `xmpp/AttachmentType.kt`
- `storage/dao/XmppConversationDao.kt` *(MODIFIED)*:
    - Added `deleteByPeerJid(peerJid: String)` query

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