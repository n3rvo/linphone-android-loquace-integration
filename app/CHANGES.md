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
```kotlin
val isVideo = if (isIncoming) {
    false // FreeSWITCH always includes video in SDP, ignore it for incoming
} else {
    LinphoneUtils.isVideoEnabled(call)
}
```

---

## `app/src/main/java/org/linphone/ui/main/contacts/viewmodel/ContactViewModel.kt`

### Change: Add loquacePresence LiveData
### Change: Set loquacePresence from SharedMainViewModel in ContactFragment

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/SharedMainViewModel.kt`

### Change: Add displayedContactPresence field
```kotlin
var displayedContactPresence: String? = null
```

---

## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactsListFragment.kt`

### Change: Pass presence to SharedMainViewModel on contact click
### Change: Populate presence map when loading contacts
### Change: Fix race condition between contact tabs

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
Added `TextInputLayout` with search icon above `chat_tab_layout` inside
`content_panel`, matching the style of the contacts search bar.

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationsListFragment.kt`

### Change: Wire up chat search bar
- `TextWatcher` on search input triggers per-tab search logic
- Chats tab: client-side filter via `applyConversationFilter()`
- Contacts tab: debounced API call (300ms) via `loadContacts()` with query
- Groups tab: client-side filter via `applyGroupFilter()`
- Search cleared on tab change, conversation open and `onPause()`
- Tab switch reapplies current query to new tab

---

## `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt`

### Change: Add search support
- Added `searchQuery: MutableLiveData<String>`
- Added `fullGroupList` private backing field for unfiltered groups
- Added `applyConversationFilter(query)` — filters from
  `LoquaceXmppManager.conversations` StateFlow using
  `conversation.displayName` with fallback to `peerJid`
- Added `applyGroupFilter(query)` — filters from `fullGroupList`
- `init` block applies current query when conversations update
- `loadGroups()` stores full list in `fullGroupList` before filtering
- `loadContacts()` accepts `query` parameter, clears list before
  new search results arrive

---

## `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt`

### Change: Call updateConversationDisplayName after resolving display name
After lazy-fetching contact or group name, calls
`LoquaceXmppManager.updateConversationDisplayName()` to persist the
resolved name so it's available for filtering immediately on next launch.

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

## `app/src/main/res/layout/chat_list_fragment.xml`

### Change: Add TabLayout, wrap content in panel, add Create Group FAB

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationsListFragment.kt`

### Change: Add XMPP chat tabs, adapter, conversation navigation and group creation

---

## `app/src/main/res/layout/main_drawer_menu.xml`

### Change: Replace Linphone drawer with Loquace custom drawer

---

## `app/src/main/java/org/linphone/ui/main/fragment/DrawerMenuFragment.kt`

### Change: Wire up Loquace drawer sections

---

## `app/src/main/res/layout/settings_fragment.xml`

### Change: Hide unwanted settings sections

---

## `app/src/main/res/layout/settings_calls.xml`

### Change: Hide unwanted call settings

---

## `app/src/main/res/layout/settings_network.xml`

### Change: Hide IPv6, add push notifications placeholder

---

## `app/src/main/res/layout/settings_advanced_fragment.xml`

### Change: Hide unwanted advanced settings, add permissions section

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

---

## `app/src/main/res/values/strings.xml`

### Change: Add file_provider_loquace string and all other new strings

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

### Change: Remove sip.linphone.org references

---

## `assets/linphonerc_factory`

### Change: Disable Linphone chat features

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
### `app/src/main/java/org/linphone/ui/main/dialer/viewmodel/LoquaceDialerViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/dialer/fragment/LoquaceDialerFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_list_cell.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_conversation_fragment.xml` *(MODIFIED)*

### Change: Add avatar and presence ring to conversation header

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppMessagesAdapter.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_incoming.xml` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_outgoing.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/LoquaceVoiceRecorder.kt` *(NEW)*
### `app/src/main/res/layout/loquace_group_details_bottom_sheet.xml` *(NEW)*
### `app/src/main/res/layout/loquace_group_member_cell.xml` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/GroupMembersAdapter.kt` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppGroupDetailsBottomSheet.kt` *(MODIFIED)*
### `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/viewmodel/LoquaceDrawerMenuViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/help/fragment/LoquaceAboutFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_about_fragment.xml` *(NEW)*
### `app/src/main/java/org/linphone/core/LoquaceFirebaseMessagingService.kt` *(NEW)*

---

## Pending features (publication checklist)
1. ~~Remove video call and chat button from SIP contact card + add status~~ ✅
2. ~~Search implementation for chat contacts and open conversations~~ ✅
3. Improve group chat participant picker (searchable + avatars + status borders)
4. Restyle of in-call screen
5. Conversation long press → delete conversation *(flagged)*
6. Push notifications toggle wiring in network settings
7. Video upload optimization
8. In-app media viewer
9. Adding special rules for phone contacts calls and emergency calls
10. Fix horizontal layout / remove landscape mode
11. Implement translation / language selection

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/LoquaceLogoutManager.kt` *(MODIFIED)* — full logout flow
- `storage/LoquaceDatabase.kt` *(MODIFIED)*:
    - Version bumped to 5
    - `MIGRATION_4_5` adds `displayName` column to `xmpp_conversations`
    - DB validity check, `needs_relogin` flag on corruption
- `storage/entity/XmppConversationEntity.kt` *(MODIFIED)*:
    - Added `displayName: String?` field
- `storage/SessionManager.kt` — `clearSession()` clears all stored data
- `sip/LoquaceSipConfigurator.kt` — push notifications enabled
- `ui/LoquaceLoginActivity.kt` *(MODIFIED)* — `OnBackPressedCallback`
  calls `moveTaskToBack(true)`
- `ui/activity_login.xml` — redesigned login screen
- `xmpp/LoquaceXmppManager.kt` *(MODIFIED)*:
    - Added `updateConversationDisplayName()` — persists resolved display
      name to StateFlow and DB
    - `loadConversations()` loads `displayName` from DB entity
    - `logout()` clears all XMPP state
- `xmpp/XmppConversation.kt` *(MODIFIED)*:
    - Added `displayName: String? = null` field
- `xmpp/XmppConnectionService.kt` — `specialUse` foreground service type
- `xmpp/XmppMessage.kt`, `xmpp/XmppHttpUploadManager.kt`,
  `xmpp/AttachmentType.kt`

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