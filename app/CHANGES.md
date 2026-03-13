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

## `app/src/main/res/layout/contacts_list_fragment.xml`

### Change 1: Add permanent search bar
**Reason:** Replace the toggled search icon in the top bar with a always-visible
search bar above the tab layout.

Added as first child inside the `lists` LinearLayout:
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    android:id="@+id/contacts_search"
    android:layout_width="match_parent"
    android:layout_height="40dp"
    android:layout_marginStart="16dp"
    android:layout_marginEnd="16dp"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="4dp"
    app:hintEnabled="false"
    app:boxStrokeWidth="0dp"
    app:boxStrokeWidthFocused="0dp"
    app:boxCornerRadiusTopStart="20dp"
    app:boxCornerRadiusTopEnd="20dp"
    app:boxCornerRadiusBottomStart="20dp"
    app:boxCornerRadiusBottomEnd="20dp"
    app:startIconDrawable="@drawable/magnifying_glass">

    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:textSize="14sp"
        android:paddingVertical="4dp"
        android:inputType="text"
        android:imeOptions="actionSearch"
        android:text="@={viewModel.searchFilter}"
        android:hint="@string/search"/>

</com.google.android.material.textfield.TextInputLayout>
```

### Change 2: Add TabLayout
**Reason:** Three contact sources (Contacts, PBX, User) shown as tabs.

Added below the search bar inside the `lists` LinearLayout:
```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/contacts_tab_layout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginStart="16dp"
    android:layout_marginEnd="16dp"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="4dp"
    app:tabMode="fixed"
    app:tabGravity="fill"/>
```

### Change 3: Hide favourites section
**Reason:** Favourites not used in Loquace. Hidden via `visibility="gone"` rather than
removed to ease future merges.

Set `android:visibility="gone"` on:
- `favourites_label`
- `favourites_contacts_list`
- `all_contacts_label`

---

## `app/src/main/java/org/linphone/ui/main/contacts/viewmodel/ContactsListViewModel.kt`

### Change 1: Force default contact filter to show all contacts
**Reason:** Phone tab should show all device contacts by default.

**Location:** `init` block, inside `coreContext.postOnCoreThread`

**Original:**
```kotlin
domainFilter = corePreferences.contactsFilter
areAllContactsDisplayed.postValue(domainFilter.isEmpty())
```

**Replaced with:**
```kotlin
domainFilter = corePreferences.contactsFilter
corePreferences.contactsFilter = ""
domainFilter = ""
areAllContactsDisplayed.postValue(true)
```

### Change 2: Hide filter icon from top bar
**Reason:** Replaced by the permanent search bar.

**Location:** `init` block
```kotlin
showFilter.value = false
```

### Change 3: Add tab state, Loquace contacts list and empty state LiveData
**Reason:** Drive which contact source is displayed based on selected tab, and
control empty state visibility across all tabs.

Added alongside existing LiveData declarations:
```kotlin
enum class ContactTab { PHONE, PBX, USER }
val currentTab = MutableLiveData<ContactTab>(ContactTab.PHONE)
val loquaceContactsList = MutableLiveData<ArrayList<ContactAvatarModel>>()
val isContactsEmpty = MutableLiveData<Boolean>(true)
```

### Change 4: Add switchTab() method
**Reason:** Handle tab selection — runs MagicSearch for Phone tab, clears search
filter when switching to Loquace tabs.
```kotlin
@UiThread
fun switchTab(tab: ContactTab) {
    currentTab.value = tab
    if (tab == ContactTab.PHONE) {
        filter()
    } else {
        searchFilter.value = ""
    }
}
```

### Change 5: Prevent MagicSearch on non-Phone tabs
**Reason:** Without this, typing in the search bar on PBX/User tabs would
also trigger Linphone's MagicSearch and show device contacts.

**Location:** `filter()` method — add early return at the top:
```kotlin
@UiThread
override fun filter() {
    if (currentTab.value != ContactTab.PHONE) return
    isListFiltered.value = currentFilter.isNotEmpty()
    coreContext.postOnCoreThread {
        applyFilter(currentFilter, domainFilter)
    }
}
```

---

## `app/src/main/java/org/linphone/ui/main/contacts/fragment/ContactsListFragment.kt`

### Change 1: Load contacts on first open after permissions
**Reason:** On first install, native contacts don't load until the second app open
because `loadContacts()` is never triggered after the permissions flow completes.

**Location:** `onResume()`
```kotlin
if (ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
) {
    (requireActivity() as MainActivity).loadContacts()
}
```

### Change 2: Wire up tabs, infinite scroll, Loquace contacts and search
**Location:** `onViewCreated()`, after all existing Linphone setup code

- Load `domain`, `token`, `userAgent` from `SessionManager`
- Setup three tabs (Contacts, PBX, User) on `contactsTabLayout`
- Tab selection calls `listViewModel.switchTab()` and `resetAndLoadContacts()` for Loquace tabs
- Infinite scroll listener on `contactsList` RecyclerView triggers `loadMoreContacts()`
- `loquaceContactsList` observer updates adapter when Loquace contacts load
- `searchFilter` observer triggers `resetAndLoadContacts()` with search term on Loquace tabs

### Change 3: Update empty state across all tabs
**Reason:** Empty state visibility was only bound to `contactsList`, so it would
stay visible when Loquace contacts loaded.

In both `contactsList` and `loquaceContactsList` observers, added at the top:
```kotlin
listViewModel.isContactsEmpty.value = it.isEmpty()
```

### Change 4: Added methods
- `resetAndLoadContacts(type, query)` — resets pagination state and loads first page
- `loadMoreContacts(type, query)` — fetches a page from Loquace API, builds Linphone
  `Friend` objects with avatars, appends to `loquaceContactsList`
- `fetchAndSaveAvatar(contact, cacheDir)` — fetches authenticated photo from API,
  saves to `filesDir`, returns local path for `friend.photo`
- `buildUserAgent(context)` — builds Loquace user agent string

---

## `app/src/main/res/values/strings.xml`

### Change: Add tab label strings
```xml
<string name="contacts_tab_phone">Contacts</string>
<string name="contacts_tab_pbx">PBX</string>
<string name="contacts_tab_user">User</string>
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
- `network/` — Retrofit API clients for auth, settings, presence and contacts
- `storage/` — Room database with SQLCipher encryption, SessionManager
- `sip/` — Linphone SIP account configurator and Core provider
- `ui/` — LoquaceLoginActivity
- `viewmodel/` — LoquaceLoginViewModel

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