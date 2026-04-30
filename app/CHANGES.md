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
LoquaceXmppManager.init(this) // Added
```

### Change: App-wide block on Night Mode
**Reason:** Loquace palette does not work well with dark colors.

**Location:** first line of `onCreate()` function
```kotlin
AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
```

---

## `app/src/main/java/org/linphone/ui/main/MainActivity.kt`

### Change: Replace first launch welcome screen with Loquace login
**Location:** `handleMainIntent()` function

### Change: Handle login result, permissions screen and contacts load
**Location:** `onActivityResult()`

### Change: Redirect to Loquace login when last account is removed
**Location:** `lastAccountRemovedEvent` observer in `onCreate()`

### Change: Add XMPP reconnection on app foreground/background
Added `onStart()` and `onStop()` to manage XMPP connection lifecycle:
```kotlin
override fun onStart() {
    super.onStart()
    val sessionManager = SessionManager(this)
    if (sessionManager.getToken() != null) {
        val serviceIntent = Intent(this, XmppConnectionService::class.java)
        startForegroundService(serviceIntent)
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

### Change: Replace Linphone's Firebase service with Loquace's
```xml
<!-- Replaced -->
<service android:name="org.linphone.core.tools.firebase.FirebaseMessaging" .../>

<!-- With -->
<service android:name="org.linphone.core.LoquaceFirebaseMessagingService"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT"/>
    </intent-filter>
</service>
```

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
- Accordion panels toggle on row click with caret up/down animation

---

## `app/src/main/java/org/linphone/ui/main/fragment/DrawerMenuFragment.kt`

### Change: Wire up Loquace drawer sections
- Added `LoquaceDrawerMenuViewModel` alongside existing `DrawerMenuViewModel`
- `loquaceViewModel.fetchData()` called when drawer opens
- Accordion toggle for Incoming Calls and Presence panels with caret animation
- Presence spinner with colored text per status (ONLINE/AWAY/BUSY/OFFLINE)
- Switch listeners update ViewModel state without submitting
- Submit buttons call `submitCallsSettings()` and `submitPresence()`
- Logout button placeholder (full implementation deferred)
- Kept all original Linphone observers (account list, profile, notifications)

---

## `app/src/main/res/layout/settings_fragment.xml`

### Change: Hide unwanted settings sections
Hidden via `android:visibility="gone"`:
- Security section
- Conversations section
- Contacts section
- Meetings section
- User Interface section
- Tunnel section
- Developer Settings

Kept: Calls, Network, Advanced Settings.

---

## `app/src/main/res/layout/settings_calls.xml`

### Change: Hide unwanted call settings
Hidden: `echo_canceller` toggle, `adaptive_rate_control`, `auto_record`,
`advanced_call_settings`.
Kept: `calibrate_echo_canceller`, `enable_video`, `vibrate`, `change_ringtone`.
Fixed constraints after hiding items.

---

## `app/src/main/res/layout/settings_network.xml`

### Change: Hide IPv6, add push notifications placeholder
Hidden: `ipv6_enabled`.
Added: `push_notifications_switch` (disabled placeholder, to be wired up when
push notifications are implemented).

---

## `app/src/main/res/layout/settings_advanced_fragment.xml`

### Change: Hide unwanted advanced settings, add permissions section
Hidden: `crashlytics`, `device_id`, `remote_provisioning`, `download_and_apply`,
`audio_devices`.
Kept: `start_at_boot`, `keep_alive_service`, `android_settings`.
Added: `permissions_title` accordion header and `permissions_list` RecyclerView
before `android_settings`.
Fixed constraints after hiding items.

---

## `app/src/main/java/org/linphone/ui/main/settings/fragment/SettingsAdvancedFragment.kt`

### Change: Add permissions section, remove audio device pickers
- Added `permissionsPanelOpen` flag
- `permissions_title` click toggles panel and swaps caret drawable
- `setupPermissionsList()` builds list of: Microphone, Camera, Contacts,
  Notifications (Android 13+), Bluetooth (Android 12+)
- Each item shows green check or red X based on grant status
- Clicking a denied permission requests it via `requestPermissionLauncher`
- List refreshes on `onResume()` if panel is open
- Removed audio device picker setup (hidden in layout)
- Removed `onPause()` device name/provisioning URL update (hidden in layout)

---

## `app/src/main/java/org/linphone/core/CorePreferences.kt`

### Change: Default `disableCallRecordings` to `true`
**Reason:** Recording toggle hidden from UI, always enable recordings.
```kotlin
val disableCallRecordings: Boolean
    get() = config.getBool("ui", "disable_call_recordings_feature", true)
```

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
<string name="settings_network_push_notifications">Enable push notifications</string>
<string name="settings_advanced_permissions_title">Permissions</string>
<string name="permission_record_audio">Microphone</string>
<string name="permission_camera">Camera</string>
<string name="permission_read_contacts">Contacts</string>
<string name="permission_post_notifications">Notifications</string>
<string name="permission_bluetooth">Bluetooth</string>
<string name="notification_incoming_call_title">Incoming Call</string>
<string name="about_app_description">Your business communication platform</string>
<string name="about_privacy_subtitle">Read our privacy policy</string>
<string name="about_based_on_linphone_title">Based on Linphone</string>
<string name="about_based_on_linphone_subtitle">This app is built on top of Linphone, an open-source VoIP project</string>
<string name="about_gpl_license_title">GNU GPL v3 License</string>
<string name="about_gpl_license_subtitle">This app is distributed under the GNU General Public License v3</string>
<string name="about_privacy_policy_url">https://your-privacy-policy-url.com</string>
<string name="about_linphone_url">https://www.linphone.org</string>
<string name="about_gpl_url">https://www.gnu.org/licenses/gpl-3.0.html</string>
```

---

## `app/build.gradle.kts`

### Change: Add Gson dependency
```kotlin
implementation(libs.gson)
```

### Change: Update applicationId to match Java predecessor
**Reason:** Allows the Kotlin app to be published as an update to the existing
Java app on the Play Store. The signing keystore must also match.
```kotlin
applicationId = "it.nems.loquacemobile" // Updated to match Java app
```

---

## `.gitignore`

### Change: Exclude google-services.json from version control
**Reason:** Contains Firebase project credentials that should not be public.