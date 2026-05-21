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

### Change: Lock app to portrait orientation
```xml
android:screenOrientation="portrait"
```
Added to `<application>` tag to apply to all activities.

### Change: Remove duplicate FileProvider

---

## `app/src/main/res/navigation/main_nav_graph.xml`

### Change: Add LoquaceDialerFragment and navigation actions

### Change: Set startDestination to loquaceDialerFragment
Avoids race conditions on first install by landing on a tab that
requires no API calls to render.

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
Stores `MainActivityTopBarBinding` reference in `initViews()`, observes
`LoquaceDrawerMenuViewModel.presenceStatus` and updates the presence ring
on the top bar avatar via `ring.setLoquacePresenceRing(status)`.

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
`resetAndLoadCalls()` exits early if `domain` or `token` are empty,
preventing infinite loading spinner on fresh installs.

### Change: Add try-catch to loadMoreCalls
Ensures `fetchInProgress` always resets to false even on API failure.

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
- Observes `presenceStatus` to update presence ring on drawer account avatar
  via `ring.setLoquacePresenceRing(status)`

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

---

## `app/src/main/java/org/linphone/core/CorePreferences.kt`

### Change: Default `disableCallRecordings` to `true`

---

## `app/src/main/res/values/strings.xml`

### Change: Add new string resources

---

## `app/src/main/res/layout/contact_avatar.xml`

### Change: Add Loquace presence ring
- Added `loquacePresence` variable of type `String`
- Added `presence_ring` ImageView overlaying the avatar

---

## `app/src/main/res/layout/contact_list_cell.xml`

### Change: Add loquacePresence variable and pass to avatar include
- Added strict tab check to prevent phone contacts overwriting Loquace tabs

---

## `app/src/main/java/org/linphone/ui/main/contacts/adapter/ContactsListAdapter.kt`

### Change: Add presence map and pass to cell binding
- Added `presenceMap: MutableMap<String, String?>` keyed by contact id
- In `ViewHolder.bind()` sets `binding.loquacePresence = presenceMap[contactModel.id]`

---

## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactsListFragment.kt`

### Change: Populate presence map when loading contacts

### Change: Fix race condition between contact tabs
- `listViewModel.contactsList` observer now strictly checks for `ContactTab.PHONE`
- `listViewModel.loquaceContactsList` observer checks for `ContactTab.PBX` or
  `ContactTab.USER` only
- `loadMoreContacts()` captures `calledForTab` at call time and discards results
  if tab has changed by the time they arrive

---

## `app/src/main/java/org/linphone/utils/DataBindingUtils.kt`

### Change: Add loquacePresence binding adapter
```kotlin
@BindingAdapter("loquacePresence")
fun ImageView.setLoquacePresenceRing(status: String?) {
    if (status.isNullOrEmpty()) {
        visibility = View.GONE
        return
    }
    val drawable = when (status.uppercase()) {
        "ONLINE" -> R.drawable.presence_ring_online
        "AWAY"   -> R.drawable.presence_ring_away
        "BUSY"   -> R.drawable.presence_ring_busy
        else     -> R.drawable.presence_ring_offline
    }
    setImageResource(drawable)
    visibility = View.VISIBLE
}
```

---

## `app/src/main/res/drawable/presence_ring_online.xml` *(NEW)*
## `app/src/main/res/drawable/presence_ring_away.xml` *(NEW)*
## `app/src/main/res/drawable/presence_ring_busy.xml` *(NEW)*
## `app/src/main/res/drawable/presence_ring_offline.xml` *(NEW)*
Oval stroke drawables for presence ring around avatars.
Colors: green (online), orange (away), red (busy), gray (offline).

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

**Key implementation:**
- `displayName` and `loquacePresence` are constructor parameters as
  `MutableLiveData` for pre-built models
- Non-group: fetches contact via `getContactByJid()`, updates `displayName`,
  `loquacePresence` and downloads avatar
- Group: fetches group list, matches by JID, updates `displayName`

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt` *(NEW)*

**Key additions:**
- `avatarModel` and `loquacePresence` populated lazily in `initialize()`
- `loadHistory()`, `loadMoreHistory()`, `deleteMessage()`, `editMessage()`,
  `markAsRead()`

### `app/src/main/res/layout/loquace_chat_conversation_fragment.xml` *(MODIFIED)*

### Change: Add avatar and presence ring to conversation header
- Added `contact_avatar` include with `app:model`, `app:hidePresence`
  and `app:loquacePresence` bindings

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppMessagesAdapter.kt` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_incoming.xml` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_outgoing.xml` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/LoquaceVoiceRecorder.kt` *(NEW)*
### `app/src/main/res/layout/loquace_group_details_bottom_sheet.xml` *(NEW)*
### `app/src/main/res/layout/loquace_group_member_cell.xml` *(MODIFIED)*

### Change: Add presence ring to group member cell
- Added `presence_ring` ImageView inside `avatar_container`

### `app/src/main/java/org/linphone/ui/main/chat/adapter/GroupMembersAdapter.kt` *(MODIFIED)*

### Change: Add presence support to group members
- Added `presenceStatuses` map keyed by account JID
- Added `updatePresence()` method
- `bind()` clips avatar to circle via custom `ViewOutlineProvider`
- `bind()` calls `setLoquacePresenceRing()` from `presenceStatuses` map

### `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppGroupDetailsBottomSheet.kt` *(MODIFIED)*

### Change: Fetch and display presence for group members
- Calls `adapter.updatePresence()` for each participant after `getContactByJid()`
- Fixed `onStart()` to use `resources.getIdentifier()` for Material bottom
  sheet view lookup

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
- `storage/entity/XmppConversationEntity.kt` *(NEW)*
- `storage/XmppConversationDao.kt` *(NEW)*
- `storage/SessionManager.kt` — `clearSession()` clears all stored data
- `sip/LoquaceSipConfigurator.kt` — `pushNotificationAllowed = true`,
  `remotePushNotificationAllowed = true`, `logout()` disables push before
  removing account
- `viewmodel/LoquaceLoginViewModel.kt` — FCM token failure handled gracefully
- `ui/LoquaceLoginActivity.kt` — loading indicator, empty field validation,
  `FLAG_ACTIVITY_CLEAR_TASK` on success
- `ui/activity_login.xml` — redesigned login screen:
    - Dark teal background (`#183a42`)
    - Loquace logo + app name + subtitle in upper half
    - White card with rounded top corners
    - `TextInputLayout` outlined fields
    - Password visibility toggle
    - Branded login button
    - Loading indicator
- `xmpp/LoquaceXmppManager.kt` — major refactor:
    - Removed contact/group maps
    - DB-backed conversation persistence
    - `disconnect()` and `logout()` clear all state
    - `init(context)` for DB access
- `xmpp/XmppConversation.kt` — simplified: removed `displayName`, `pictureUrl`
- `xmpp/XmppConnectionService.kt` — `specialUse` foreground service type
- `xmpp/XmppMessage.kt` — `isRetracted`, `senderName`, `isUploading`,
  `formattedTime`
- `xmpp/XmppHttpUploadManager.kt` — XEP-0363 HTTP file upload
- `xmpp/AttachmentType.kt` — NONE, IMAGE, VIDEO, AUDIO, FILE, VOICE_NOTE

**New drawables in `loquace-integration`:**
- `loquace_logo.xml` — brand logo vector
- `login_circle_bg.xml` — decorative circle for login background
- `login_card_background.xml` — white card with rounded top corners

**New colors in `loquace-integration`:**
- `login_primary` — `#183a42`
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