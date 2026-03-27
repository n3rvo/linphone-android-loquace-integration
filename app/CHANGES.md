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

### Change: Add TabLayout and wrap content in panel

---

## `app/src/main/java/org/linphone/ui/main/chat/fragment/ConversationsListFragment.kt`

### Change: Add XMPP chat tabs, adapter and conversation navigation
- Added `xmppViewModel` and `xmppAdapter`
- Setup three tabs (Chats, Contacts, Groups)
- Conversation click navigates to `xmppConversationFragment`
- Added `showChatsTab()`, `showContactsTab()`, `showGroupsTab()`

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
### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppConversationsAdapter.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationsListViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/viewmodel/XmppConversationViewModel.kt` *(NEW)*
### `app/src/main/java/org/linphone/ui/main/chat/adapter/XmppMessagesAdapter.kt` *(NEW)*

**Key implementation notes:**
- Images loaded via `LoquaceMediaDownloader` with auth headers
- Camera photos/videos load instantly from local path
- Videos cached locally after first download for faster reopening
- Attachment type detection strips query parameters before checking extension
- All audio formats (mp3, m4a, ogg, wav, mka) treated as VOICE_NOTE
- Visibility of attachment views controlled entirely in code, not data binding
- `attachmentClickedEvent` fires when tapping image, video or file bubble
- Upload progress shown via `CircularProgressIndicator` while `isUploading=true`
- Sent message IDs tracked in `sentMessageIds` to avoid carbon copy duplicates
- `addPendingMessage()` shows message immediately while uploading
- `updateMessage()` updates pending message after upload completes

### `app/src/main/res/layout/loquace_chat_bubble_incoming.xml` *(NEW)*
### `app/src/main/res/layout/loquace_chat_bubble_outgoing.xml` *(NEW)*

**Attachment views:** `attachment_image`, `attachment_video`, `attachment_file`,
`attachment_voice`, `upload_progress` — all default `gone`, visibility set in adapter.
Voice note bubble has `voice_play_button` and `voice_seekbar`.

### `app/src/main/res/layout/loquace_chat_conversation_fragment.xml` *(NEW)*
Chat screen with header, message list, attach button, text input, send button
and mic button. Send/mic toggle based on text input content. Recording area
shows timer and hint while recording.

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

### `app/src/main/java/org/linphone/ui/main/chat/LoquaceVoiceRecorder.kt` *(NEW)*
Wraps Android `MediaRecorder`. Records in MP3/AAC format.
Methods: `startRecording()`, `stopRecording()`, `cancelRecording()`, `isRecording()`.

### `app/src/main/java/org/linphone/ui/main/history/model/LoquaceCallLogModel.kt` *(NEW)*

---

## Pending features
- Chat contacts tab (fetch from Loquace API, show XMPP-enabled contacts)
- Chat groups tab
- Push notifications (requires updated `google-services.json`)
- Video upload optimization for longer videos
- In-app media viewer (future phase)
- Message history via MAM XEP-0313 (future phase)

---

## New Module: `loquace-integration`

Entirely new module — no merge conflicts expected here.

**Key files:**
- `network/` — Retrofit API clients for auth, settings, presence, contacts, call history, media
- `network/MediaApi.kt` — authenticated media download endpoint
- `network/LoquaceMediaDownloader.kt` — downloads media bytes with auth headers
- `network/ContactResponse.kt` — added `chats: List<ContactChat>?` field for XMPP JID
- `storage/` — Room DB with SQLCipher, SessionManager (includes `saveUserAgent`/`getUserAgent`)
- `sip/LoquaceSipConfigurator.kt` — sets SIP user agent via `core.setUserAgent()`
- `ui/` — LoquaceLoginActivity
- `viewmodel/LoquaceLoginViewModel.kt` — full login flow including XMPP
- `xmpp/LoquaceXmppManager.kt` — Smack XMPP singleton with:
    - Message store per conversation (`_messages` StateFlow)
    - `isConnecting` flag to prevent double connection
    - `sentMessageIds` set to ignore carbon copies of sent messages
    - Roster loading disabled
    - Resource set to user agent
    - Attachment type detection (strips query params, all audio → VOICE_NOTE)
    - `addPendingMessage()` — shows message immediately while uploading
    - `updateMessage()` — updates pending message after upload completes
    - `uploadAndSendFile()` — creates pending message, uploads, then updates
    - `sendMessageWithAttachment()` — for sending attachment messages
- `xmpp/XmppConnectionService.kt` — foreground service with user agent from intent/SessionManager
- `xmpp/XmppConnectionState.kt` — sealed class for connection states
- `xmpp/XmppMessage.kt` — data class with attachment fields, `isUploading`, `formattedTime`
- `xmpp/XmppConversation.kt` — data class for conversations
- `xmpp/XmppHttpUploadManager.kt` — XEP-0363 HTTP file upload via Smack
- `xmpp/AttachmentType.kt` — enum: NONE, IMAGE, VIDEO, AUDIO, FILE, VOICE_NOTE
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