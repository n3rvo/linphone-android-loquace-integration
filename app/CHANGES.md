# Loquace Modifications to Linphone Android

This file documents all modifications made to the original Linphone Android codebase.
When merging from upstream `release/6.2`, recheck and reapply these changes.

---

## `app/src/main/java/org/linphone/LinphoneApplication.kt`

### Change: Initialize LoquaceCoreProvider after Core starts
**Reason:** Allows the `loquace-integration` module to access the Linphone Core without
directly importing `LinphoneApplication`.
```kotlin
coreContext = CoreContext(context)
coreContext.start()
LoquaceCoreProvider.init { coreContext.core } // Added
```

### Change: App-wide block on Night Mode
**Reason:** Loquace palette does not work well with dark colors.

**Location:** first line of `onCreate()` function
```kotlin
AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Added this
```

---

## `app/src/main/java/org/linphone/ui/main/MainActivity.kt`

### Change: Replace first launch welcome screen with Loquace login
**Location:** `handleMainIntent()` function

### Change: Handle login result, permissions screen and contacts load
**Location:** `onActivityResult()`

### Change: Redirect to Loquace login when last account is removed
**Location:** `lastAccountRemovedEvent` observer in `onCreate()`

---

## `app/src/main/AndroidManifest.xml`

### Change: Register XMPP foreground service and permissions
```xml
<service
    android:name="org.linphone.loquace_integration.xmpp.XmppConnectionService"
    android:foregroundServiceType="dataSync"
    android:exported="false"/>

<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

### Change: Remove duplicate FileProvider
**Reason:** Using Linphone's existing one with authority `@string/file_provider`
pointing to `@xml/provider_paths`.

---

## `app/src/main/res/navigation/main_nav_graph.xml`

### Change: Add LoquaceDialerFragment and navigation actions
Added `loquaceDialerFragment` with actions to/from all other main tabs.

---

## `app/src/main/res/navigation/chat_nav_graph.xml`

### Change: Add XmppConversationFragment
```xml
<fragment
    android:id="@+id/xmppConversationFragment"
    android:name="org.linphone.ui.main.chat.fragment.XmppConversationFragment"
    android:label="XmppConversationFragment"
    tools:layout="@layout/loquace_chat_conversation_fragment">
    <argument android:name="peerJid" app:argType="string" />
    <argument android:name="displayName" app:argType="string" />
    <argument android:name="isGroup" app:argType="boolean" android:defaultValue="false" />
</fragment>

<action
android:id="@+id/action_global_xmppConversationFragment"
app:destination="@id/xmppConversationFragment"
app:enterAnim="@anim/slide_in_right"
app:exitAnim="@anim/slide_out_left"
app:popEnterAnim="@anim/slide_in_left"
app:popExitAnim="@anim/slide_out_right"/>
```

---

## `app/src/main/res/layout/bottom_nav_bar.xml`

### Change: Add Dialer tab between Contacts and Calls
Added dialer tab and updated `calls` start constraint.

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/AbstractMainViewModel.kt`

### Change: Add dialer navigation support
Added `dialerSelected`, `navigateToDialerEvent` and `navigateToDialer()`.

---

## `app/src/main/java/org/linphone/ui/main/fragment/AbstractMainFragment.kt`

### Change 1: Add dialer navigation
Added `goToDialer()` and `R.id.loquaceDialerFragment` cases to all navigation methods.

### Change 2: Add initViews overload without SlidingPaneLayout
For `LoquaceDialerFragment` which doesn't need a sliding pane.

### Change 3: Update tab selection state
Updated `currentlyDisplayedFragment` observer to include `dialerSelected`.

---

## `app/src/main/res/layout/history_list_fragment.xml`

### Change: Add TabLayout, wrap in panel, fix empty state, hide FAB

---

## `app/src/main/java/org/linphone/ui/main/history/viewmodel/HistoryListViewModel.kt`

### Change: Add tab state and Loquace call history LiveData
Added `HistoryTab` enum, `currentTab`, `loquaceCallLogs`, `isHistoryEmpty`, `switchTab()`.

---

## `app/src/main/java/org/linphone/ui/main/history/fragment/HistoryListFragment.kt`

### Change: Replace Linphone call history with Loquace API call history
Setup two tabs, infinite scroll, avatar fetching, callback calls.
Added `resetAndLoadCalls()`, `loadMoreCalls()`, `fetchAndSaveAvatar()`, `buildUserAgent()`.

---

## `app/src/main/java/org/linphone/ui/main/history/adapter/HistoryListAdapter.kt`

### Change: Add Loquace call log view type
Added `LOQUACE_CALL_TYPE`, related events, `LoquaceCallLogViewHolder`.

---

## `app/src/main/java/org/linphone/ui/main/history/model/CallLogModelWrapper.kt`

### Change: Add loquaceCallLogModel field

---

## `app/src/main/res/layout/history_list_cell.xml`

### Change: Add loquaceModel variable and update bindings

---

## `app/src/main/res/layout/chat_list_fragment.xml`

### Change 1: Add TabLayout and wrap content in panel

### Change 2: Add Create Group FAB
```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/new_group"
    android:visibility="gone"
    android:src="@drawable/users_three"
    .../>
```
Shown only when Groups tab is active.

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationsListFragment.kt`

### Change: Add XMPP chat tabs, adapter, conversation navigation and group creation
- Added `xmppViewModel` and `xmppAdapter`
- Setup three tabs (Chats, Contacts, Groups) with FAB visibility per tab
- Conversation/contact/group click navigates to `xmppConversationFragment`
- Added `showChatsTab()`, `showContactsTab()`, `showGroupsTab()`
- Added `showCreateGroupDialog()` — dialog for group name input
- Added `showContactPickerForGroup()` — paginated contact picker with checkboxes
- Added `createGroup()` — creates group via API and invites selected participants
- Added `buildUserAgent()` helper
- `isFetchingContacts` and `isFetchingGroups` observers toggle `fetchInProgress`

---

## `app/src/main/res/layout/main_drawer_menu.xml`

### Change: Replace Linphone drawer with Loquace custom drawer
Completely replaced with new Loquace drawer layout containing:
- Original Linphone header (app name + logo + close button)
- Original Linphone account list with avatar and profile button
- Incoming Calls accordion section (Mobile/Browser/Phone switches with icons)
- Presence accordion section (status spinner with colors + message input)
- Settings row → navigates to Linphone's existing settings fragment
- About row → navigates to about fragment
- Language row → navigates to settings fragment
- Logout button at bottom (red, styled like other rows)
- Rounded right corners via `drawer_background.xml` drawable
- Accordion panels toggle on row click (collapsed by default)
- Submit buttons styled as compact pill-shaped text buttons

---

## `app/src/main/java/org/linphone/ui/main/fragment/DrawerMenuFragment.kt`

### Change: Wire up Loquace drawer sections
- Added `LoquaceDrawerMenuViewModel` alongside existing `DrawerMenuViewModel`
- `loquaceViewModel.fetchData()` called when drawer opens
- Accordion toggle for Incoming Calls and Presence panels
- Presence spinner with colored text per status (ONLINE/AWAY/BUSY/OFFLINE)
- Switch listeners update ViewModel state without submitting
- Submit buttons call `submitCallsSettings()` and `submitPresence()`
- Logout button placeholder (full implementation deferred)
- Kept all original Linphone observers (account list, profile, notifications)

---

## `app/src/main/res/values/strings.xml`

### Change: Add new string resources
```xml
<string name="contacts_tab_phone">Contacts</string>
<string name="contacts_tab_pbx">PBX</string>
<string name="contacts_tab_user">User</string>
<string name="history_tab_all">All</string>
<string name="history_tab_missed">Missed</string>
<string name="bottom_navigation_dialer_label">Dialer</string>
<string name="chat_tab_chats">Chats</string>
<string name="chat_tab_contacts">Contacts</string>
<string name="chat_tab_groups">Groups</string>
<string name="voice_note_release_to_send">Release to send</string>
<string name="voice_note_permission_denied">Microphone permission is required to record voice notes</string>
<string name="attachment_picker_title">Attach</string>
<string name="attachment_picker_image">Image</string>
<string name="attachment_picker_video">Video</string>
<string name="attachment_picker_file">File</string>
<string name="attachment_picker_camera_photo">Camera Photo</string>
<string name="attachment_picker_camera_video">Camera Video</string>
<string name="group_name_hint">Group name</string>
<string name="create_group_title">Create Group</string>
<string name="add_participants_title">Add Participants</string>
<string name="next">Next</string>
<string name="cancel">Cancel</string>
<string name="create">Create</string>
<string name="content_description_group_create">Create new group</string>
<string name="group_members">members</string>
<string name="group_add_member">Add Member</string>
<string name="group_remove_member">Remove member</string>
<string name="group_delete">Delete Group</string>
<string name="group_delete_confirmation">Are you sure you want to delete this group? This action cannot be undone.</string>
<string name="group_remove_member_confirmation">Are you sure you want to remove %1$s from the group?</string>
<string name="delete">Delete</string>
<string name="loading">Loading...</string>
<string name="message_edit">Edit message</string>
<string name="message_delete">Delete message</string>
<string name="message_delete_confirmation">Are you sure you want to delete this message for everyone?</string>
<string name="save">Save</string>
<string name="confirm">Confirm</string>
<string name="drawer_incoming_calls_title">Incoming Calls</string>
<string name="drawer_device_mobile">Mobile</string>
<string name="drawer_device_browser">Browser</string>
<string name="drawer_device_phone">Phone</string>
<string name="drawer_presence_title">Presence</string>
<string name="drawer_presence_status_label">Status</string>
<string name="drawer_presence_message_label">Status message</string>
<string name="drawer_presence_message_hint">Add a status message...</string>
<string name="drawer_status_online">Online</string>
<string name="drawer_status_away">Away</string>
<string name="drawer_status_busy">Busy</string>
<string name="drawer_status_offline">Offline</string>
<string name="drawer_about_title">About</string>
<string name="drawer_language_title">Language</string>
<string name="drawer_logout">Logout</string>
<string name="drawer_submit">Save</string>
```

---

## `app/src/main/res/drawable/drawer_background.xml` *(NEW)*
Custom drawable with rounded right corners for the drawer panel.

---

## `app/src/main/res/values/styles.xml`

### Change: Add LoquaceSwitch style
```xml
<style name="LoquaceSwitch" parent="Widget.MaterialComponents.CompoundButton.Switch">
    <item name="colorPrimary">?attr/color_main1_500</item>
    <item name="colorSwitchThumbNormal">?attr/color_main2_200</item>
    <item name="android:colorForeground">?attr/color_main2_200</item>
</style>
```

---

## `app/build.gradle.kts`

### Change: Add Gson dependency
```kotlin
implementation(libs.gson)
```

---

## New files in `app` module

### `app/src/main/res/layout/loquace_dialer_fragment.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/dialer/viewmodel/LoquaceDialerViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/dialer/fragment/LoquaceDialerFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_list_cell.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt` *(NEW)*

**Key implementation notes:**
- `prebuiltAvatarModel` parameter for pre-fetched avatars (contacts tab)
- Falls back to `LoquaceXmppManager.getContactName()` for display name
- Uses cached avatar file via `LoquaceXmppManager.getContactId()` → `avatar_{contactId}.jpg`
- `subject` uses `displayName ?? getContactName(peerJid) ?? peerJid`

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*

**Key methods:**
- `loadContacts()` — fetches chat-enabled contacts via `chats=true`, paginated,
  with avatar pre-fetching, stores `contactId`, `contactName` in `LoquaceXmppManager`
- `loadGroups()` — fetches groups from Loquace API

### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt` *(NEW)*

**Key additions:**
- `isLoadingHistory`, `hasMoreHistory`, `oldestMessageUid`
- `loadHistory()` — fetches MAM history, prepends to existing messages
- `loadMoreHistory()` — triggered by scroll to top, loads next page
- `isPrependingHistory` flag — prevents scroll-to-bottom when prepending
- `deleteMessage()` — calls `LoquaceXmppManager.retractMessage()`
- `editMessage()` — calls `LoquaceXmppManager.editMessage()`

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppMessagesAdapter.kt` *(NEW)*

**Key implementation notes:**
- Images loaded via `LoquaceMediaDownloader` with auth headers
- Camera photos/videos load instantly from local path
- Videos cached locally after first download for faster reopening
- Attachment type detection strips query parameters before checking extension
- All audio formats (mp3, m4a, ogg, wav, mka) treated as VOICE_NOTE
- Visibility of attachment views controlled entirely in code, not data binding
- `attachmentClickedEvent` fires when tapping image, video or file bubble
- `messageLongPressedEvent` fires on long press of outgoing bubble
- Upload progress shown via `CircularProgressIndicator` while `isUploading=true`
- Retracted messages shown in gray italic text
- Sent message IDs tracked in `sentMessageIds` to avoid carbon copy duplicates

### `app/src/main/res/layout/loquace_chat_bubble_incoming.xml` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_outgoing.xml` *(NEW)*

**Attachment views:** `attachment_image`, `attachment_video`, `attachment_file`,
`attachment_voice`, `upload_progress` — all default `gone`, visibility set in adapter.
Voice note bubble has `voice_play_button` and `voice_seekbar`.
Incoming bubble has `sender_name` TextView (visible only when `senderName` is not null).

### `app/src/main/res/layout/loquace_chat_conversation_fragment.xml` *(NEW)*
Chat screen with header, message list, attach button, text input, send/mic buttons
in a `FrameLayout` container, recording area with timer, and `history_progress`
indicator at top of messages list.

### `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppConversationFragment.kt` *(NEW)*
Extends `SlidingPaneChildFragment`. Handles:
- File picking (Image, Video, File, Camera Photo, Camera Video)
- File upload via `LoquaceXmppManager.uploadAndSendFile()`
- Message sending
- Attachment tap → `openMediaFullScreen()` or `openDocument()`
- Full screen media via Android's built-in viewer with `FileProvider`
- Document opening via `ACTION_VIEW` intent
- Voice note recording via hold-to-record mic button
- `RECORD_AUDIO` permission handling
- Recording timer display
- Group room joining via `LoquaceXmppManager.joinRoom()` with `displayName`
- Group details bottom sheet via header tap
- Scroll listener → `loadMoreHistory()` when reaching top
- Long press on outgoing bubble → context menu (Edit / Delete)
- `showMessageContextMenu()`, `showEditMessageDialog()`, `deleteMessage()`

### `app/src/main/java/org/linphone/ui/main/chat/LoquaceVoiceRecorder.kt` *(NEW)*
### `app/src/main/res/layout/loquace_group_details_bottom_sheet.xml` *(NEW)*
### `app/src/main/res/layout/loquace_group_member_cell.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/GroupMembersAdapter.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppGroupDetailsBottomSheet.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW)*

### `app/src/main/java/org/linphone/ui/main/viewmodel/LoquaceDrawerMenuViewModel.kt` *(NEW)*
Handles presence and incoming calls API calls for the drawer menu.

**Key methods:**
- `fetchData()` — fetches presence and calls settings from API on drawer open
- `submitPresence()` — POSTs updated status and message to `status/presence`
- `submitCallsSettings()` — POSTs full inbound devices object to `settings/calls`

---

## Pending features
- About screen implementation
- Language selection implementation
- Logout implementation
- Round avatars in group details screen
- Conversation list avatars for single chats
- Top bar avatar from `profile.avatarUrl`
- Improved contact picker with avatars and search (post-prototype)
- Push notifications (requires updated `google-services.json`)
- Video upload optimization for longer videos
- In-app media viewer (future phase)
- Contact list caching (post-prototype optimization)

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/` — Retrofit API clients for auth, settings, presence, contacts, call history, media, chats
- `network/MediaApi.kt` — authenticated media download endpoint
- `network/LoquaceMediaDownloader.kt` — downloads media bytes with auth headers
- `network/ContactResponse.kt` — includes `chats: List<ContactChat>?` for XMPP JID,
  `ContactPhone.status` nullable
- `network/GroupResponse.kt` — group and participant data models,
  `participants` defaults to `emptyList()`
- `network/ChatsApi.kt` — group CRUD endpoints
- `network/LoquaceGroupsRepository.kt` — group API calls + `fetchChatEnabledContacts()`
    + `getContactByJid()`
- `network/SettingsRepository.kt` — added `sessionManager` parameter, saves `avatarUrl`
- `network/SettingsApi.kt` — added `updateCallsSettings()` POST endpoint
- `network/PresenceApi.kt` — added `updatePresence()` POST endpoint
- `network/PresenceResponse.kt` — all fields made nullable
- `network/SettingsResponse.kt` — `Profile` fields made nullable
- `storage/SessionManager.kt` — added `saveAvatarUrl()`, `getAvatarUrl()`
- `storage/entity/PresenceEntity.kt` — all fields made nullable
- `storage/entity/ProfileEntity.kt` — all fields made nullable
- `sip/LoquaceSipConfigurator.kt` — added `context` parameter, sets avatar
  from `my_avatar.jpg` on account params after login
- `viewmodel/LoquaceLoginViewModel.kt` — FCM token failure handled gracefully,
  downloads and caches `my_avatar.jpg`, passes `context` to configure()
- `xmpp/LoquaceXmppManager.kt` — Smack XMPP singleton with:
    - Message store per conversation (`_messages` StateFlow)
    - `isConnecting` flag to prevent double connection
    - `sentMessageIds` set to ignore carbon copies
    - `roomsWithListeners` set to prevent duplicate MUC message listeners
    - Roster loading disabled, resource set to user agent
    - Attachment type detection (strips query params, all audio → VOICE_NOTE)
    - `addPendingMessage()`, `updateMessage()`, `uploadAndSendFile()`
    - `uploadAndSendFile()` uses `sentMessage.stanzaId` for consistent IDs
      in both 1-1 (via `conn.sendStanza()`) and MUC (via `muc.createMessage()`)
    - `joinRoom()` — joins MUC, stores group name, updates conversation display name,
      only adds message listener once per room via `roomsWithListeners`
    - `groupNames`, `contactNames`, `contactIds`, `contactPictureUrls` maps
    - `prependMessages()` — prepends history preserving MAM order
    - `fetchMessageHistory()` — MAM XEP-0313, paginated, detects and merges
      retracted messages, uses `message.stanzaId` as ID
    - `retractMessage()` — sends XEP-0424 retraction with body fallback,
      clears attachment fields, updates local store
    - `retractLocalMessage()` — clears body, attachment fields, sets `isRetracted=true`
    - `editMessage()` — sends XEP-0308 correction, updates local store
    - `updateMessageBody()` — updates local message body on correction received
    - `sendMessage()` uses `sentMessage.stanzaId` for consistent message IDs
    - Incoming and MUC listeners handle retraction and correction stanzas
    - Group display name shown via `groupNames` map
- `xmpp/XmppMessage.kt` — added `isRetracted: Boolean = false`, `senderName`,
  `isUploading`, `formattedTime`
- `xmpp/XmppConversation.kt` — includes `displayName`, `pictureUrl`, `isGroup`
- `xmpp/XmppHttpUploadManager.kt` — XEP-0363 HTTP file upload
- `xmpp/AttachmentType.kt` — NONE, IMAGE, VIDEO, AUDIO, FILE, VOICE_NOTE
- `network/LoquaceAvatarHelper.kt` — shared avatar fetch utility

**Dependencies added:**
- `retrofit2:retrofit`
- `retrofit2:converter-gson`
- `androidx.security:security-crypto`
- `net.zetetic:sqlcipher-android`
- `androidx.sqlite:sqlite`
- `androidx.room:room-runtime`
- `androidx.room:room-ktx`
- `androidx.room:room-compiler`
- `com.google.firebase:firebase-messaging`
- `org.linphone:linphone-sdk-android`
- `org.igniterealtime.smack:smack-android:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-tcp:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-im:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-extensions:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-sasl-provided:4.4.8` (xpp3 excluded)