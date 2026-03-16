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

## `app/src/main/res/navigation/main_nav_graph.xml`

### Change: Add global action for StartCallFragment
**Reason:** Makes the dialer accessible from any fragment via the bottom nav bar.

Added alongside other global actions:
```xml
<action
    android:id="@+id/action_global_startCallFragment"
    app:destination="@id/startCallFragment"
    app:enterAnim="@anim/slide_in"
    app:popExitAnim="@anim/slide_out"
    app:launchSingleTop="true"/>
```

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

### Change: Add dialer navigation observer and update tab selection state
**Reason:** Handle dialer navigation event and highlight dialer tab when active.

Added in `setViewModel()` alongside existing navigation observers:
```kotlin
viewModel.navigateToDialerEvent.observe(viewLifecycleOwner) {
    it.consume {
        if (findNavController().currentDestination?.id != R.id.startCallFragment) {
            findNavController().navigate(R.id.action_global_startCallFragment)
        }
    }
}
```

Updated `currentlyDisplayedFragment` observer:
```kotlin
sharedViewModel.currentlyDisplayedFragment.observe(viewLifecycleOwner) {
    viewModel.contactsSelected.value = it == R.id.contactsListFragment
    viewModel.callsSelected.value = it == R.id.historyListFragment
    viewModel.conversationsSelected.value = it == R.id.conversationsListFragment
    viewModel.meetingsSelected.value = it == R.id.meetingsListFragment
    viewModel.dialerSelected.value = it == R.id.startCallFragment
}
```

---

## `app/src/main/res/layout/history_list_fragment.xml`

### Change 1: Add TabLayout and wrap content in panel with rounded corners
**Reason:** Two call history tabs (All/Missed) and visual continuity with top bar.

Replaced standalone `RecyclerView` with a `LinearLayout` wrapper:
```xml
<LinearLayout
    android:id="@+id/content_panel"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:orientation="vertical"
    android:background="@drawable/shape_squircle_white_r20_top_background"
    android:layout_marginTop="@dimen/top_bar_height"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toTopOf="@id/bottom_nav_bar"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/history_tab_layout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/transparent"
        app:tabMode="fixed"
        app:tabGravity="fill"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/history_list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="@android:color/transparent"/>

</LinearLayout>
```

### Change 2: Fix empty state constraints
**Reason:** After wrapping RecyclerView in LinearLayout, empty state views lost their
reference. Updated to constrain to `content_panel` instead of `history_list`.
```xml
app:layout_constraintTop_toTopOf="@id/content_panel"
app:layout_constraintBottom_toBottomOf="@id/content_panel"
```

### Change 3: Hide FAB
**Reason:** Replaced by the Dialer tab in the bottom nav bar.
```xml
android:visibility="gone"
```

---

## `app/src/main/java/org/linphone/ui/main/history/viewmodel/HistoryListViewModel.kt`

### Change: Add tab state and Loquace call history LiveData
**Reason:** Drive which call history source is displayed based on selected tab.

Added alongside existing LiveData:
```kotlin
enum class HistoryTab { ALL, MISSED }
val currentTab = MutableLiveData<HistoryTab>(HistoryTab.ALL)
val loquaceCallLogs = MutableLiveData<ArrayList<CallLogModelWrapper>>()
val isHistoryEmpty = MutableLiveData<Boolean>(true)
```

Added method:
```kotlin
@UiThread
fun switchTab(tab: HistoryTab) {
    currentTab.value = tab
}
```

---

## `app/src/main/java/org/linphone/ui/main/history/fragment/HistoryListFragment.kt`

### Change: Replace Linphone call history with Loquace API call history
**Reason:** Show calls from Loquace backend instead of Linphone's local call logs.

- Load credentials from `SessionManager`
- Setup two tabs (All, Missed) on `historyTabLayout`
- Tab selection calls `listViewModel.switchTab()` and `resetAndLoadCalls()`
- Infinite scroll listener triggers `loadMoreCalls()`
- `callLogs.observe` replaced with no-op
- Added `loquaceCallBackClickedEvent` observer for callback calls

Added methods:
- `resetAndLoadCalls(missedOnly)` — resets pagination and loads first page
- `loadMoreCalls(missedOnly)` — fetches page from API, builds `LoquaceCallLogModel`
  objects with avatars, appends to `loquaceCallLogs`
- `buildUserAgent(context)` — builds Loquace user agent string

---

## `app/src/main/java/org/linphone/ui/main/history/adapter/HistoryListAdapter.kt`

### Change: Add Loquace call log view type
**Reason:** Render Loquace API call entries in the same list as Linphone call logs.

- Added `LOQUACE_CALL_TYPE = 3` constant
- Added `loquaceCallBackClickedEvent` and `loquaceCallClickedEvent` LiveData
- Added `LoquaceCallLogViewHolder` using `HistoryListCellBinding`
- Updated `getItemViewType()` to handle `isLoquaceCall`
- Updated `onCreateViewHolder()` and `onBindViewHolder()` for new type
- Updated `CallLogDiffCallback` to handle Loquace items

---

## `app/src/main/java/org/linphone/ui/main/history/model/CallLogModelWrapper.kt`

### Change: Add loquaceCallLogModel field
**Reason:** Support Loquace API call entries alongside Linphone call logs.
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

## `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW FILE)*

**Reason:** UI model for Loquace API call entries, mirrors `CallLogModel` interface
so the same adapter cell layout works for both.

**Location:** `org.linphone.ui.main.history.model`

---

## `app/src/main/res/layout/history_list_cell.xml`

### Change: Add loquaceModel variable and update bindings
**Reason:** Support rendering both Linphone and Loquace call entries in the same cell.

Added variable:
```xml
<variable
    name="loquaceModel"
    type="org.linphone.ui.main.history.model.LoquaceCallLogModel" />
```

Updated bindings to use whichever model is set:
```xml
bind:model="@{model != null ? model.avatarModel : loquaceModel.avatarModel}"
android:text="@{model != null ? model.avatarModel.name : loquaceModel.contactName}"
android:src="@{model != null ? model.iconResId : loquaceModel.iconResId}"
android:text="@{model != null ? model.dateTime : loquaceModel.dateTime}"
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
```

---

## `app/build.gradle.kts`

### Change: Add Gson dependency
**Reason:** Used in `ContactsListFragment` for avatar path handling.
```kotlin
implementation(libs.gson)
```

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Contains:**
- `network/` — Retrofit API clients for auth, settings, presence, contacts and call history
- `storage/` — Room database with SQLCipher encryption, SessionManager
- `sip/` — Linphone SIP account configurator and Core provider
- `ui/` — LoquaceLoginActivity
- `viewmodel/` — LoquaceLoginViewModel

**Key files added during contacts and call history implementation:**
- `network/ContactResponse.kt`
- `network/ContactsApi.kt`
- `network/LoquaceContactsRepository.kt`
- `network/CallHistoryResponse.kt`
- `network/CallHistoryApi.kt`
- `network/LoquaceCallHistoryRepository.kt`
- `network/LoquaceAvatarHelper.kt`

**Dependencies added (not in original Linphone):**
- `retrofit2:retrofit`
- `retrofit2:converter-gson`
- `androidx.security:security-crypto`
- `net.zetetic:sqlcipher-android`
- `androidx.sqlite:sqlite`
- `androidx.room:room-runtime`
- `androidx.room:room-ktx`
- `androidx.room:room-compiler`
- `com.google.firebase:firebase-messaging`
- `org.linphone:linphone-sdk-android` (same as app module)