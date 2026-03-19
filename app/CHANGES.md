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
`LoquaceLoginActivity`. On first launch, instead of showing Linphone's welcome screen,
the app goes directly to Loquace login when no accounts are configured.

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
**Reason:** After successful Loquace login, navigate to Linphone's existing permissions
fragment via `AssistantActivity`. After permissions are granted, trigger native contacts
load immediately without requiring an app restart.

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
**Reason:** When the user logs out or the last SIP account is removed, the app should
return to Loquace login instead of Linphone's assistant.

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
**Reason:** `XmppConnectionService` keeps the XMPP connection alive in the background.

Added inside `<application>` tag:
```xml
<service
    android:name="org.linphone.loquace_integration.xmpp.XmppConnectionService"
    android:foregroundServiceType="dataSync"
    android:exported="false"/>
```

Added permissions:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
```

---

## `app/src/main/res/navigation/main_nav_graph.xml`

### Change: Add LoquaceDialerFragment and its navigation actions
**Reason:** Custom dialer as a peer tab alongside Contacts, Calls and Conversations.

Added `loquaceDialerFragment` with actions to all other main tabs:
```xml
<fragment
    android:id="@+id/loquaceDialerFragment"
    android:name="org.linphone.ui.main.dialer.fragment.LoquaceDialerFragment"
    android:label="LoquaceDialerFragment"
    tools:layout="@layout/loquace_dialer_fragment">
    <action
        android:id="@+id/action_loquaceDialerFragment_to_historyListFragment"
        app:destination="@id/historyListFragment"
        app:launchSingleTop="true"
        app:popUpTo="@id/loquaceDialerFragment"
        app:popUpToInclusive="true" />
    <action
        android:id="@+id/action_loquaceDialerFragment_to_contactsListFragment"
        app:destination="@id/contactsListFragment"
        app:launchSingleTop="true"
        app:popUpTo="@id/loquaceDialerFragment"
        app:popUpToInclusive="true" />
    <action
        android:id="@+id/action_loquaceDialerFragment_to_conversationsListFragment"
        app:destination="@id/conversationsListFragment"
        app:launchSingleTop="true"
        app:popUpTo="@id/loquaceDialerFragment"
        app:popUpToInclusive="true" />
    <action
        android:id="@+id/action_loquaceDialerFragment_to_meetingsListFragment"
        app:destination="@id/meetingsListFragment"
        app:launchSingleTop="true"
        app:popUpTo="@id/loquaceDialerFragment"
        app:popUpToInclusive="true" />
</fragment>
```

Added action to dialer from each existing main fragment:
- `historyListFragment` → `action_historyListFragment_to_loquaceDialerFragment`
- `contactsListFragment` → `action_contactsListFragment_to_loquaceDialerFragment`
- `conversationsListFragment` → `action_conversationsListFragment_to_loquaceDialerFragment`
- `meetingsListFragment` → `action_meetingsListFragment_to_loquaceDialerFragment`

---

## `app/src/main/res/layout/bottom_nav_bar.xml`

### Change: Add Dialer tab between Contacts and Calls
**Reason:** Replaces the floating action button in the history fragment with a
permanent dialer entry in the bottom navigation bar.

Added between `contacts` and `calls` views:
```xml
<androidx.appcompat.widget.AppCompatTextView
    style="@style/bottom_nav_bar_label_style"
    android:id="@+id/dialer"
    android:onClick="@{() -> viewModel.navigateToDialer()}"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:paddingTop="9dp"
    android:paddingBottom="8dp"
    android:drawableTop="@drawable/phone_plus"
    android:background="@drawable/squircle_transparent_button_background"
    android:drawablePadding="4dp"
    android:drawableTint="@{viewModel.dialerSelected ? @color/main1_500 : @color/main2_600, default=@color/main2_600}"
    android:text="@string/bottom_navigation_dialer_label"
    textFont="@{viewModel.dialerSelected ? NotoSansFont.NotoSansBold : NotoSansFont.NotoSansRegular}"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintEnd_toStartOf="@id/calls"
    app:layout_constraintStart_toEndOf="@id/contacts"
    app:layout_constraintTop_toTopOf="parent" />
```

Updated `calls` start constraint:
```xml
app:layout_constraintStart_toEndOf="@id/dialer"
```

---

## `app/src/main/java/org/linphone/ui/main/viewmodel/AbstractMainViewModel.kt`

### Change: Add dialer navigation support
**Reason:** Drive navigation to the dialer from the bottom nav bar.

Added alongside existing LiveData:
```kotlin
val dialerSelected = MutableLiveData<Boolean>()

val navigateToDialerEvent: MutableLiveData<Event<Boolean>> by lazy {
    MutableLiveData()
}
```

Added alongside existing navigate methods:
```kotlin
@UiThread
fun navigateToDialer() {
    navigateToDialerEvent.value = Event(true)
}
```

---

## `app/src/main/java/org/linphone/ui/main/fragment/AbstractMainFragment.kt`

### Change 1: Add dialer navigation
**Reason:** Handle navigation to and from the dialer tab.

Added `goToDialer()` method and `R.id.loquaceDialerFragment` cases to all existing
navigation methods (`goToContactsList`, `goToHistoryList`, `goToConversationsList`,
`goToMeetingsList`).

Added in `setViewModel()`:
```kotlin
viewModel.navigateToDialerEvent.observe(viewLifecycleOwner) {
    it.consume {
        if (currentFragmentId != R.id.loquaceDialerFragment) {
            goToDialer()
        }
    }
}
```

### Change 2: Add initViews overload without SlidingPaneLayout
**Reason:** `LoquaceDialerFragment` doesn't need a sliding pane.
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
**Location:** `currentlyDisplayedFragment` observer in `setViewModel()`
```kotlin
sharedViewModel.currentlyDisplayedFragment.observe(viewLifecycleOwner) {
    viewModel.contactsSelected.value = it == R.id.contactsListFragment
    viewModel.callsSelected.value = it == R.id.historyListFragment
    viewModel.conversationsSelected.value = it == R.id.conversationsListFragment
    viewModel.meetingsSelected.value = it == R.id.meetingsListFragment
    viewModel.dialerSelected.value = it == R.id.loquaceDialerFragment
}
```

---

## `app/src/main/res/layout/history_list_fragment.xml`

### Change 1: Add TabLayout and wrap content in panel with rounded corners
**Reason:** Two call history tabs (All/Missed) and visual continuity with top bar.

Replaced standalone `RecyclerView` with a `LinearLayout` wrapper containing
`TabLayout` and `RecyclerView`, both with transparent backgrounds.
Empty state constraints updated to reference `content_panel`.

### Change 2: Hide FAB
**Reason:** Replaced by the Dialer tab in the bottom nav bar.
```xml
android:visibility="gone"
```

---

## `app/src/main/java/org/linphone/ui/main/history/viewmodel/HistoryListViewModel.kt`

### Change: Add tab state and Loquace call history LiveData
Added: `HistoryTab` enum, `currentTab`, `loquaceCallLogs`, `isHistoryEmpty`, `switchTab()`

---

## `app/src/main/java/org/linphone/ui/main/history/fragment/HistoryListFragment.kt`

### Change: Replace Linphone call history with Loquace API call history
- Load credentials from `SessionManager`
- Setup two tabs (All, Missed)
- Infinite scroll, avatar fetching, callback calls
- `callLogs.observe` replaced with no-op
- Added `resetAndLoadCalls()`, `loadMoreCalls()`, `fetchAndSaveAvatar()`, `buildUserAgent()`

---

## `app/src/main/java/org/linphone/ui/main/history/adapter/HistoryListAdapter.kt`

### Change: Add Loquace call log view type
- Added `LOQUACE_CALL_TYPE = 3`
- Added `loquaceCallBackClickedEvent` and `loquaceCallClickedEvent`
- Added `LoquaceCallLogViewHolder`
- Updated `getItemViewType()`, `onCreateViewHolder()`, `onBindViewHolder()`, `CallLogDiffCallback`

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
Added `loquaceModel` variable, updated avatar, name, icon, datetime bindings to
use whichever model is set.

---

## `app/src/main/res/layout/chat_list_fragment.xml`

### Change: Add TabLayout and wrap content in panel with rounded corners
**Reason:** Three chat tabs (Chats, Contacts, Groups) and visual continuity with top bar.

Replaced standalone `RecyclerView` with a `LinearLayout` wrapper:
```xml
<LinearLayout
    android:id="@+id/content_panel"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:orientation="vertical"
    android:background="@drawable/shape_squircle_white_r20_top_background"
    android:layout_marginTop="@dimen/top_bar_height"
    ...>

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/chat_tab_layout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/transparent"
        app:tabMode="fixed"
        app:tabGravity="fill"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/conversations_list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="@android:color/transparent"/>

</LinearLayout>
```

Empty state constraints updated to reference `content_panel`.

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationsListFragment.kt`

### Change: Add XMPP chat tabs and adapter
**Reason:** Replace Linphone chat with Loquace XMPP conversations.

- Added `xmppViewModel` and `xmppAdapter` properties
- Setup three tabs (Chats, Contacts, Groups)
- `xmppViewModel.conversations` observer updates `xmppAdapter`
- Added `showChatsTab()`, `showContactsTab()`, `showGroupsTab()` methods
- Contacts and Groups tabs stubbed for future phases

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
ViewModel for custom dialer. Extends `AbstractMainViewModel`. Handles digit input,
backspace, zero long press for `+`, initiates audio calls.

### `app/src/main/java/org/linphone/ui/main/dialer/fragment/LoquaceDialerFragment.kt` *(NEW)*
Fragment for custom dialer. Extends `AbstractMainFragment`. Wires up top bar,
bottom nav bar and navigation via `initViews()`.

### `app/src/main/res/layout/loquace_chat_list_cell.xml` *(NEW)*
Conversation list cell layout bound to `XmppConversationModel`. Simplified version
of Linphone's `chat_list_cell.xml` without Linphone-specific fields.

### `app/src/main/java/org/linphone/ui/main/chat/model/XmppConversationModel.kt` *(NEW)*
UI model for XMPP conversations. Mirrors `ConversationModel` interface so the
same cell layout structure works.

### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*
RecyclerView adapter for XMPP conversations using `loquace_chat_list_cell.xml`.

### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*
ViewModel for XMPP conversations list. Extends `AbstractMainViewModel`. Collects
from `LoquaceXmppManager.conversations` StateFlow and exposes as LiveData.

### `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW)*
UI model for Loquace API call entries. Mirrors `CallLogModel` interface.

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/` — Retrofit API clients for auth, settings, presence, contacts, call history
- `storage/` — Room DB with SQLCipher, SessionManager
- `sip/` — SIP configurator and Core provider
- `ui/` — LoquaceLoginActivity
- `viewmodel/` — LoquaceLoginViewModel
- `xmpp/LoquaceXmppManager.kt` — Smack XMPP connection singleton
- `xmpp/XmppConnectionService.kt` — Foreground service for background connection
- `xmpp/XmppConnectionState.kt` — Sealed class for connection states
- `xmpp/XmppMessage.kt` — Data class for XMPP messages
- `xmpp/XmppConversation.kt` — Data class for XMPP conversations

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
- `org.igniterealtime.smack:smack-android:4.4.8`
- `org.igniterealtime.smack:smack-tcp:4.4.8`
- `org.igniterealtime.smack:smack-im:4.4.8`
- `org.igniterealtime.smack:smack-extensions:4.4.8`
- `org.igniterealtime.smack:smack-sasl-provided:4.4.8`