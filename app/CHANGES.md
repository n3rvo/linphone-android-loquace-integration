# Loquace Modifications to Linphone Android

This file documents all modifications made to the original Linphone Android codebase.
When merging from upstream `release/6.2`, recheck and reapply these changes.

---

## `app/src/main/java/org/linphone/LinphoneApplication.kt`

### Change: Initialize LoquaceCoreProvider after Core starts
```kotlin
coreContext = CoreContext(context)
coreContext.start()
LoquaceCoreProvider.init { coreContext.core }
LoquaceXmppManager.init(this)
```

### Change: App-wide block on Night Mode
```kotlin
AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
```

---

## `app/src/main/java/org/linphone/ui/main/MainActivity.kt`

### Change: Replace first launch welcome screen with Loquace login
**Location:** `handleMainIntent()`

### Change: Handle login result, permissions screen and contacts load
**Location:** `onActivityResult()`

### Change: Redirect to Loquace login when last account is removed
**Location:** `lastAccountRemovedEvent` observer

### Change: Add XMPP reconnection on app foreground/background
```kotlin
override fun onStart() {
    super.onStart()
    val sessionManager = SessionManager(this)
    if (sessionManager.getToken() != null) {
        startForegroundService(Intent(this, XmppConnectionService::class.java))
    }
}

override fun onStop() {
    super.onStop()
    LoquaceXmppManager.disconnect()
    stopService(Intent(this, XmppConnectionService::class.java))
}
```

---

## `app/src/main/AndroidManifest.xml`

### Change: Register XMPP foreground service as specialUse
```xml
<service
    android:name="org.linphone.loquace_integration.xmpp.XmppConnectionService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Needed to maintain XMPP connection for chat messages."/>
</service>
```

### Change: Replace Linphone's Firebase service with Loquace's
```xml
<service android:name="org.linphone.core.LoquaceFirebaseMessagingService"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT"/>
    </intent-filter>
</service>
```

### Change: Remove duplicate FileProvider

---

## `app/src/main/res/navigation/main_nav_graph.xml`

### Change: Add LoquaceDialerFragment and navigation actions

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

---

## `app/src/main/res/layout/history_list_fragment.xml`

### Change: Add TabLayout, wrap in panel, fix empty state, hide FAB

---

## `app/src/main/java/org/linphone/ui/main/history/viewmodel/HistoryListViewModel.kt`

### Change: Add tab state and Loquace call history LiveData

---

## `app/src/main/java/org/linphone/ui/main/history/fragment/HistoryListFragment.kt`

### Change: Replace Linphone call history with Loquace API call history

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
- Original Linphone header and account list preserved
- Incoming Calls accordion (Mobile/Browser/Phone switches with icons)
- Presence accordion (status spinner with colors + message input)
- Settings, About, Language rows
- Logout button wired to `LoquaceLogoutManager` with confirmation dialog
- Rounded right corners via `drawer_background.xml`
- Accordion panels toggle with caret animation

---

## `app/src/main/java/org/linphone/ui/main/fragment/DrawerMenuFragment.kt`

### Change: Wire up Loquace drawer sections
- Added `LoquaceDrawerMenuViewModel`
- Fetch data on drawer open
- Accordion toggles with caret animation
- Presence spinner with colored text per status
- Submit buttons for calls and presence
- Logout button wired to `LoquaceLogoutManager.logout()` with confirmation dialog
- Navigates to login with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`

---

## `app/src/main/res/layout/settings_fragment.xml`

### Change: Hide unwanted settings sections
Hidden: Security, Conversations, Contacts, Meetings, User Interface, Tunnel,
Developer Settings. Kept: Calls, Network, Advanced Settings.

---

## `app/src/main/res/layout/settings_calls.xml`

### Change: Hide unwanted call settings
Hidden: `echo_canceller` toggle, `adaptive_rate_control`, `auto_record`,
`advanced_call_settings`. Fixed constraints.

---

## `app/src/main/res/layout/settings_network.xml`

### Change: Hide IPv6, add push notifications placeholder
Hidden: `ipv6_enabled`. Added disabled `push_notifications_switch` placeholder.

---

## `app/src/main/res/layout/settings_advanced_fragment.xml`

### Change: Hide unwanted advanced settings, add permissions section
Hidden: `crashlytics`, `device_id`, `remote_provisioning`, `audio_devices`.
Added: `permissions_title` accordion and `permissions_list` RecyclerView.

---

## `app/src/main/java/org/linphone/ui/main/settings/fragment/SettingsAdvancedFragment.kt`

### Change: Add permissions section, remove audio device pickers
Permissions: Microphone, Camera, Contacts, Notifications, Bluetooth.
Green check / red X icons. Click denied → request. Caret animation on accordion.

---

## `app/src/main/java/org/linphone/core/CorePreferences.kt`

### Change: Default `disableCallRecordings` to `true`
```kotlin
val disableCallRecordings: Boolean
    get() = config.getBool("ui", "disable_call_recordings_feature", true)
```

---

## `app/src/main/res/values/strings.xml`

### Change: Add new string resources
All Loquace-specific strings including drawer, settings, chat, permissions,
about, notification, and logout confirmation strings.

---

## `app/build.gradle.kts`

### Change: Add Gson dependency and update applicationId
```kotlin
implementation(libs.gson)
applicationId = "it.nems.loquacemobile"
```

---

## `.gitignore`

### Change: Exclude google-services.json from version control
File added to .gitignore: `google-services.json`
Also untracked via: `git rm --cached app/google-services.json`

---

## `assets/linphonerc_default`

### Change: Remove sip.linphone.org references
- Removed `rls_uri=sips:rls@sip.linphone.org`
- Cleared `contacts_filter`
- Removed Linphone file transfer and version check URLs

---

## `assets/linphonerc_factory`

### Change: Disable Linphone chat features
- Set `use_cpim=0`
- Set `chat_messages_aggregation=0`

---

## New files in `app` module

### `app/src/main/res/drawable/loquace_logo.xml` *(NEW)*
Brand logo vector — white chat bubble shape, used in login screen,
about page and drawer header.

### `app/src/main/res/drawable/loquace_splashscreen.xml` *(NEW)*
Brand logo adapted for splash screen, sized to fit 60x60 viewport
with `scaleX/scaleY=0.5`, filled with `@color/orange_main_500`.

### `app/src/main/res/drawable/drawer_background.xml` *(NEW)*
### `app/src/main/res/drawable/ic_permission_granted.xml` *(NEW)*
### `app/src/main/res/drawable/ic_permission_denied.xml` *(NEW)*
### `app/src/main/res/layout/loquace_permission_item.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/settings/LoquacePermissionsAdapter.kt` *(NEW)*
### `app/src/main/res/layout/loquace_dialer_fragment.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/dialer/viewmodel/LoquaceDialerViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/dialer/fragment/LoquaceDialerFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_list_cell.xml` *(NEW)*

**Key binding:** `android:text="@{model.displayName}"` — `MutableLiveData<String>`
updated lazily via API.

### `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt` *(NEW)*

**Key implementation:**
- `displayName` is `MutableLiveData<String>` initialized to JID prefix
- Non-group: fetches contact via `getContactByJid()`, updates `displayName`
  and downloads avatar to `avatar_$contactId.jpg`
- Group: fetches group list, matches by JID, updates `displayName`
- Avatar loaded via `model.picturePath.postValue()` after download
- `onBindViewHolder` observes `avatarModel.picturePath` to trigger rebind

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*

**Key implementation:**
- Observes `avatarModel.picturePath` in `onBindViewHolder` to trigger rebind
  when avatar downloads complete

### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*

**Key implementation:**
- No longer populates `LoquaceXmppManager` maps
- `loadContacts()` passes `displayName` as `MutableLiveData` directly
- `loadGroups()` passes group name as `MutableLiveData` directly

### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt` *(NEW)*

**Key additions:**
- `loadHistory()`, `loadMoreHistory()`, `isPrependingHistory`
- `deleteMessage()`, `editMessage()`
- `markAsRead()` — resets unread count in StateFlow and DB
- `avatarModel: MutableLiveData<ContactAvatarModel>` — populated lazily
  in `initialize()` via `getContactByJid()` for contacts, group name for groups

### `app/src/main/res/layout/loquace_chat_conversation_fragment.xml` *(MODIFIED)*

### Change: Add avatar to conversation header
- Added `<include layout="@layout/contact_avatar">` between back button and title
- Passes `app:model="@{viewModel.avatarModel}"` and `app:hidePresence="@{true}"`
- Updated `title` constraints to start from avatar end

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppMessagesAdapter.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_incoming.xml` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_outgoing.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/LoquaceVoiceRecorder.kt` *(NEW)*
### `app/src/main/res/layout/loquace_group_details_bottom_sheet.xml` *(NEW)*
### `app/src/main/res/layout/loquace_group_member_cell.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/GroupMembersAdapter.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppGroupDetailsBottomSheet.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/viewmodel/LoquaceDrawerMenuViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/help/fragment/LoquaceAboutFragment.kt` *(NEW)*
### `app/src/main/res/layout/loquace_about_fragment.xml` *(NEW)*
### `app/src/main/java/org/linphone/core/LoquaceFirebaseMessagingService.kt` *(NEW)*

**Push routing:**
- `call-id` present → SIP push → `super.onMessageReceived()`
- `subject=chat` → XMPP chat push → `showChatNotification()`
- else → SIP without call-id → `super.onMessageReceived()`

**Note:** Android 15 `dataSync` foreground service quota may cause
`CorePushService` to crash during heavy testing — expected to be fixed
in a future Linphone SDK `5.5.x` patch.

---

## Pending features
- Conversation long press → delete conversation *(flagged)*
- Push notifications toggle wiring in network settings
- Language selection implementation
- Improved contact picker with avatars and search
- Video upload optimization
- In-app media viewer

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/` — Retrofit API clients for auth, settings, presence, contacts,
  call history, media, chats
- `network/SettingsApi.kt` — added `updateCallsSettings()` POST
- `network/PresenceApi.kt` — added `updatePresence()` POST
- `network/LoquaceGroupsRepository.kt` — `fetchChatEnabledContacts()`,
  `getContactByJid()`, `getGroups()`
- `network/LoquaceLogoutManager.kt` *(NEW)* — coordinates full logout:
    - XMPP disconnect and state clear
    - SIP account removal from Linphone Core
    - Linphone config file deletion
    - DB conversations clear
    - Avatar files clear
    - Session clear
- `storage/LoquaceDatabase.kt` — version 4, `XmppConversationEntity`,
  `MIGRATION_2_3`, `MIGRATION_3_4`
- `storage/entity/XmppConversationEntity.kt` *(NEW)* — minimal fields:
  `peerJid`, `lastMessage`, `lastTimestamp`, `unreadCount`, `isGroup`
- `storage/XmppConversationDao.kt` *(NEW)* — `getAll`, `upsert`, `delete`,
  `deleteAll`
- `storage/SessionManager.kt` — `clearSession()` clears all stored data
- `sip/LoquaceSipConfigurator.kt` — added `pushNotificationAllowed = true`,
  `remotePushNotificationAllowed = true`, updated `logout()` to disable
  push before removing account
- `viewmodel/LoquaceLoginViewModel.kt` — FCM token failure handled gracefully
- `ui/LoquaceLoginActivity.kt` — loading indicator, empty field validation,
  `FLAG_ACTIVITY_CLEAR_TASK` on success
- `ui/activity_login.xml` — redesigned login screen:
    - Dark teal background (`#183a42`)
    - Loquace logo + app name + subtitle in upper half
    - White card with rounded top corners
    - `TextInputLayout` outlined fields for domain, username, password
    - Password visibility toggle
    - Branded login button
    - Loading indicator
- `xmpp/LoquaceXmppManager.kt` — major refactor:
    - Removed contact/group maps entirely
    - `loadConversations()` loads minimal data from DB
    - `updateConversationWithMessage()` persists minimal data to DB
    - `markConversationAsRead()` resets unread in StateFlow and DB
    - `disconnect()` clears connection state
    - `logout()` clears all state including conversations and messages
    - `init(context)` for DB access
- `xmpp/XmppConversation.kt` — simplified: removed `displayName`, `pictureUrl`
- `xmpp/XmppConnectionService.kt` — `specialUse` foreground service type
- `xmpp/XmppMessage.kt` — `isRetracted`, `senderName`, `isUploading`,
  `formattedTime`
- `xmpp/XmppHttpUploadManager.kt` — XEP-0363 HTTP file upload
- `xmpp/AttachmentType.kt` — NONE, IMAGE, VIDEO, AUDIO, FILE, VOICE_NOTE

**New drawables in `loquace-integration`:**
- `loquace_logo.xml` — brand logo vector (white chat bubble shape)
- `login_circle_bg.xml` — decorative circle for login background
- `login_card_background.xml` — white card with rounded top corners

**New colors in `loquace-integration`:**
- `login_primary` — `#183a42` (dark teal brand color)
- `login_primary_dark` — `#0F262C`
- `login_label` — `#666666`
- `login_error` — `#D32F2F`

**Dependencies added to `loquace-integration`:**
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
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `com.google.android.material:material:1.11.0`
- `org.igniterealtime.smack:smack-android:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-tcp:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-im:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-extensions:4.4.8` (xpp3 excluded)
- `org.igniterealtime.smack:smack-sasl-provided:4.4.8` (xpp3 excluded)