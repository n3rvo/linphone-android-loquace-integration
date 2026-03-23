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
**Reason:** Linphone's `WelcomeActivity` and account assistant are replaced by
`LoquaceLoginActivity`.

**Location:** `handleMainIntent()` function

**Original:**
```kotlin
if (corePreferences.firstLaunch) {
    Log.i("$TAG First time Linphone 6.0 has been started, showing Welcome activity")
    corePreferences.firstLaunch = false
    coreContext.postOnMainThread {
        startActivity(Intent(this, WelcomeActivity::class.java))
    }
} else if (core.accountList.isEmpty()) {
    Log.w("$TAG No account found, showing Assistant activity")
    coreContext.postOnMainThread {
        startActivity(Intent(this, LoquaceLoginActivity::class.java))
    }
}
```

**Replaced with:**
```kotlin
if (core.accountList.isEmpty()) {
    Log.w("$TAG No account found, showing Loquace login")
    coreContext.postOnMainThread {
        startActivityForResult(
            Intent(this, LoquaceLoginActivity::class.java),
            REQUEST_LOGIN
        )
    }
}
```

### Change: Handle login result, permissions screen and contacts load
**Location:** `onActivityResult()` override, `REQUEST_LOGIN` and `REQUEST_PERMISSIONS` constants
```kotlin
private const val REQUEST_LOGIN = 1001
private const val REQUEST_PERMISSIONS = 1002

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
        val showPermissions = data?.getBooleanExtra(
            LoquaceLoginActivity.SHOW_PERMISSIONS, false
        ) ?: false

        if (showPermissions) {
            val intent = Intent(this, AssistantActivity::class.java).apply {
                putExtra(AssistantActivity.SKIP_LANDING_EXTRA, true)
            }
            startActivityForResult(intent, REQUEST_PERMISSIONS)
        } else {
            goToLatestVisitedFragment()
        }
    } else if (requestCode == REQUEST_PERMISSIONS) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadContacts()
        }
        goToLatestVisitedFragment()
    }
}
```

### Change: Redirect to Loquace login when last account is removed
**Location:** `lastAccountRemovedEvent` observer in `onCreate()`
```kotlin
viewModel.lastAccountRemovedEvent.observe(this) {
    it.consume {
        startActivity(Intent(this, LoquaceLoginActivity::class.java))
    }
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
```

---

## `app/src/main/res/navigation/main_nav_graph.xml`

### Change: Add LoquaceDialerFragment and its navigation actions
Added `loquaceDialerFragment` with actions to all other main tabs and actions
from each existing main fragment to the dialer.

---

## `app/src/main/res/navigation/chat_nav_graph.xml`

### Change: Add XmppConversationFragment
```xml
<fragment
    android:id="@+id/xmppConversationFragment"
    android:name="org.linphone.ui.main.chat.fragment.XmppConversationFragment"
    android:label="XmppConversationFragment"
    tools:layout="@layout/loquace_chat_conversation_fragment">
    <argument
        android:name="peerJid"
        app:argType="string" />
    <argument
        android:name="displayName"
        app:argType="string" />
    <argument
        android:name="isGroup"
        app:argType="boolean"
        android:defaultValue="false" />
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
Added `navigateToDialerEvent` observer in `setViewModel()`.

### Change 2: Add initViews overload without SlidingPaneLayout
```kotlin
fun initViews(
    topBar: MainActivityTopBarBinding,
    navBar: BottomNavBarBinding,
    @IdRes fragmentId: Int
) {
    navigationBar = navBar.root
    initSearchBar(topBar.search)
    initNavigation(fragmentId)
}
```

### Change 3: Update tab selection state
Updated `currentlyDisplayedFragment` observer to include `dialerSelected`.

---

## `app/src/main/res/layout/history_list_fragment.xml`

### Change: Add TabLayout, wrap in panel, fix empty state, hide FAB
Replaced standalone `RecyclerView` with `LinearLayout` wrapper containing
`TabLayout` and `RecyclerView`. Empty state constraints updated to reference
`content_panel`. FAB set to `visibility="gone"`.

---

## `app/src/main/java/org/linphone/ui/main/history/viewmodel/HistoryListViewModel.kt`

### Change: Add tab state and Loquace call history LiveData
Added `HistoryTab` enum, `currentTab`, `loquaceCallLogs`, `isHistoryEmpty`, `switchTab()`.

---

## `app/src/main/java/org/linphone/ui/main/history/fragment/HistoryListFragment.kt`

### Change: Replace Linphone call history with Loquace API call history
- Setup two tabs (All, Missed)
- Infinite scroll, avatar fetching, callback calls
- `callLogs.observe` replaced with no-op
- Added `resetAndLoadCalls()`, `loadMoreCalls()`, `fetchAndSaveAvatar()`, `buildUserAgent()`

---

## `app/src/main/java/org/linphone/ui/main/history/adapter/HistoryListAdapter.kt`

### Change: Add Loquace call log view type
Added `LOQUACE_CALL_TYPE`, `loquaceCallBackClickedEvent`, `loquaceCallClickedEvent`,
`LoquaceCallLogViewHolder`. Updated `getItemViewType()`, `onCreateViewHolder()`,
`onBindViewHolder()`, `CallLogDiffCallback`.

---

## `app/src/main/java/org/linphone/ui/main/history/model/CallLogModelWrapper.kt`

### Change: Add loquaceCallLogModel field
```kotlin
class CallLogModelWrapper(
    val callLogModel: CallLogModel? = null,
    val contactModel: ConversationContactOrSuggestionModel? = null,
    val loquaceCallLogModel: LoquaceCallLogModel? = null
) {
    val isCallLog = callLogModel != null
    val isContactOrSuggestion = contactModel != null
    val isLoquaceCall = loquaceCallLogModel != null
}
```

---

## `app/src/main/res/layout/history_list_cell.xml`

### Change: Add loquaceModel variable and update bindings
Added `loquaceModel` variable, updated bindings to use whichever model is set.

---

## `app/src/main/res/layout/chat_list_fragment.xml`

### Change: Add TabLayout and wrap content in panel
Replaced standalone `RecyclerView` with `LinearLayout` wrapper containing
`TabLayout` and `RecyclerView`. Empty state constraints updated to reference
`content_panel`.

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationsListFragment.kt`

### Change: Add XMPP chat tabs, adapter and conversation navigation
- Added `xmppViewModel` and `xmppAdapter`
- Setup three tabs (Chats, Contacts, Groups)
- `xmppViewModel.conversations` observer updates `xmppAdapter`
- Conversation click navigates to `xmppConversationFragment` via `chatNavContainer`
- Added `showChatsTab()`, `showContactsTab()`, `showGroupsTab()`
- Commented out `sharedViewModel.showConversationEvent` in existing adapter click

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
Custom dialer layout with number display, dialpad grid, call button, backspace,
top bar and bottom nav bar.

### `app/src/main/java/org/linphone/ui/main/dialer/viewmodel/LoquaceDialerViewModel.kt` *(NEW)*
ViewModel for custom dialer. Extends `AbstractMainViewModel`.

### `app/src/main/java/org/linphone/ui/main/dialer/fragment/LoquaceDialerFragment.kt` *(NEW)*
Fragment for custom dialer. Extends `AbstractMainFragment`.

### `app/src/main/res/layout/loquace_chat_list_cell.xml` *(NEW)*
Conversation list cell bound to `XmppConversationModel`.

### `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt` *(NEW)*
UI model for XMPP conversations.

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*
RecyclerView adapter for XMPP conversations.

### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*
ViewModel for XMPP conversations list. Extends `AbstractMainViewModel`.

### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt` *(NEW)*
ViewModel for individual XMPP conversation screen.

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppMessagesAdapter.kt` *(NEW)*
RecyclerView adapter for XMPP messages with incoming/outgoing view types.

### `app/src/main/res/layout/loquace_chat_bubble_incoming.xml` *(NEW)*
Incoming message bubble layout bound to `XmppMessage`.

### `app/src/main/res/layout/loquace_chat_bubble_outgoing.xml` *(NEW)*
Outgoing message bubble layout bound to `XmppMessage`.

### `app/src/main/res/layout/loquace_chat_conversation_fragment.xml` *(NEW)*
Chat conversation layout with header, message list and send area.

### `app/src/main/java/org/linphone/ui/main/chat/fragment/XmppConversationFragment.kt` *(NEW)*
Fragment for XMPP chat screen. Extends `SlidingPaneChildFragment`.

### `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW)*
UI model for Loquace API call entries.

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/` — Retrofit API clients for auth, settings, presence, contacts, call history
- `storage/` — Room DB with SQLCipher, SessionManager
- `sip/LoquaceSipConfigurator.kt` — SIP configurator, sets user agent via `core.setUserAgent()`
- `ui/` — LoquaceLoginActivity
- `viewmodel/LoquaceLoginViewModel.kt` — Login flow including XMPP connection and service start
- `xmpp/LoquaceXmppManager.kt` — Smack XMPP singleton, message store per conversation
- `xmpp/XmppConnectionService.kt` — Foreground service, reads user agent from intent or SessionManager
- `xmpp/XmppConnectionState.kt` — Sealed class for connection states
- `xmpp/XmppMessage.kt` — Data class with `formattedTime` computed property
- `xmpp/XmppConversation.kt` — Data class for XMPP conversations
- `network/LoquaceAvatarHelper.kt` — Shared avatar fetch utility

**Key changes to existing loquace-integration files:**
- `LoquaceSipConfigurator` — added `userAgent` parameter, sets via `core.setUserAgent()`
- `SessionManager` — added `saveUserAgent()` and `getUserAgent()`
- `LoquaceLoginViewModel` — saves user agent, passes to SIP configurator, XMPP manager and service
- `LoquaceXmppManager` — added message store per conversation, `isConnecting` flag,
  roster loading disabled, resource set to user agent

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