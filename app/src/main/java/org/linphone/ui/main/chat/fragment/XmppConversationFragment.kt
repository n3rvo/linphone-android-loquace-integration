package org.linphone.ui.main.chat.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.LoquaceChatConversationFragmentBinding
import org.linphone.loquace_integration.network.GroupResponse
import org.linphone.loquace_integration.network.LoquaceGroupsRepository
import org.linphone.loquace_integration.network.LoquaceMediaDownloader
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.xmpp.AttachmentType
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager
import org.linphone.loquace_integration.xmpp.XmppHttpUploadManager
import org.linphone.loquace_integration.xmpp.XmppMessage
import org.linphone.ui.main.chat.LoquaceVoiceRecorder
import org.linphone.ui.main.chat.adapter.XmppMessagesAdapter
import org.linphone.ui.main.chat.viewmodel.XmppConversationViewModel
import org.linphone.ui.main.fragment.SlidingPaneChildFragment
import org.linphone.utils.Event
import java.io.File

@UiThread
class XmppConversationFragment : SlidingPaneChildFragment() {

    companion object {
        private const val TAG = "[Xmpp Conversation Fragment]"
    }

    private lateinit var binding: LoquaceChatConversationFragmentBinding
    private lateinit var viewModel: XmppConversationViewModel
    private lateinit var adapter: XmppMessagesAdapter

    private var currentGroup: GroupResponse? = null

    private val args: XmppConversationFragmentArgs by navArgs()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedFiles(uris)
        }
    }

    private var cameraImageUri: Uri? = null
    private var cameraVideoUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                val file = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (file.length() > 0) {
                    handleSelectedFile(file, "image/jpeg")  // Pass file and mimeType directly
                } else {
                    Log.e("$TAG Camera file is empty")
                }
            }
        }
    }

    private val cameraVideoLauncher = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            cameraVideoUri?.let { uri ->
                val file = File(requireContext().cacheDir, "video_${System.currentTimeMillis()}.mp4")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (file.length() > 0) {
                    handleSelectedFile(file, "video/mp4")  // Pass file and mimeType directly
                } else {
                    Log.e("$TAG Camera video file is empty")
                }
            }
        }
    }

    private val voiceRecorder by lazy { LoquaceVoiceRecorder(requireContext()) }
    private var recordingStartTime = 0L
    private var recordingTimerJob: kotlinx.coroutines.Job? = null

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("$TAG Record audio permission granted")
        } else {
            Log.w("$TAG Record audio permission denied")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.loquace_chat_conversation_fragment,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[XmppConversationViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        adapter = XmppMessagesAdapter()
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.messagesList.layoutManager = layoutManager
        binding.messagesList.adapter = adapter

        // Initialize with args
        viewModel.initialize(
            jid   = args.peerJid,
            name  = args.displayName,
            group = args.isGroup
        )
        viewModel.markAsRead()

        if (args.isGroup) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val myJid = LoquaceXmppManager.getMyJid()
                val nickname = myJid.substringBefore("@")
                LoquaceXmppManager.joinRoom(args.peerJid, nickname, args.displayName)

                val sessionManager = SessionManager(requireContext())
                val domain = sessionManager.getDomain() ?: ""
                val token = sessionManager.getToken() ?: ""
                val userAgent = sessionManager.getUserAgent()

                currentGroup = LoquaceGroupsRepository().getGroupDetails(domain, token, userAgent, args.peerJid)
                Log.d("GroupDetails", "currentGroup set: ${currentGroup?.name}, participants=${currentGroup?.participants?.size}")
            }
        }

        binding.title.setOnClickListener {
            val group = currentGroup
            Log.d("GroupDetails", "Title clicked, currentGroup=${group?.name}")
            if (group != null) {
                XmppGroupDetailsBottomSheet(group) {
                    // Group deleted - go back
                    goBack()
                }.show(parentFragmentManager, "GroupDetails")
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d(TAG, "Messages updated: ${messages.size} items")
            val previousCount = adapter.itemCount

            adapter.submitList(messages) {
                if (viewModel.isPrependingHistory) {
                    viewModel.isPrependingHistory = false
                    val newItems = adapter.itemCount - previousCount
                    if (newItems > 0) {
                        (binding.messagesList.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(newItems, 0)
                    }
                } else {
                    binding.messagesList.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        binding.back.setOnClickListener {
            goBack()
        }

        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text?.toString() ?: ""
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.messageInput.setText("")
            }
        }

        binding.attachButton.setOnClickListener {
            showAttachmentPicker()
        }

        adapter.attachmentClickedEvent.observe(viewLifecycleOwner) {
            it.consume { message ->
                when {
                    message.isImage || message.isVideo -> openMediaFullScreen(message)
                    message.isFile -> openDocument(message)
                }
            }
        }

        adapter.messageLongPressedEvent.observe(viewLifecycleOwner) {
            it.consume { message ->
                showMessageContextMenu(message)
            }
        }

        // Wire up text input to toggle send/mic button
        binding.messageInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val isEmpty = s.isNullOrEmpty()
                binding.sendButton.visibility = if (isEmpty) View.GONE else View.VISIBLE
                binding.micButton.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
        })

        // Initially hide send button, show mic
        binding.sendButton.visibility = View.GONE
        binding.micButton.visibility = View.VISIBLE

        // Hold to record
        setupMicButton()

        // Load more history when scrolling to top
        binding.messagesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (layoutManager.findFirstVisibleItemPosition() == 0 && dy < 0) {
                    viewModel.loadMoreHistory()
                }
            }
        })

        // Show loading indicator while fetching history
        viewModel.isLoadingHistory.observe(viewLifecycleOwner) { isLoading ->
            binding.historyProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun goBack(): Boolean {
        sharedViewModel.closeSlidingPaneEvent.value = Event(true)
        return true
    }

    private fun showAttachmentPicker() {
        val options = arrayOf(
            getString(R.string.attachment_picker_image),
            getString(R.string.attachment_picker_video),
            getString(R.string.attachment_picker_file),
            getString(R.string.attachment_picker_camera_photo),
            getString(R.string.attachment_picker_camera_video)
        )
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Attach")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> filePickerLauncher.launch("image/*")
                    1 -> filePickerLauncher.launch("video/*")
                    2 -> filePickerLauncher.launch("application/*")
                    3 -> launchCamera()
                    4 -> launchCameraVideo()
                }
            }
            .show()
    }

    private fun launchCamera() {
        val photoFile = File(
            requireContext().cacheDir,
            "photo_${System.currentTimeMillis()}.jpg"
        )
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().getString(R.string.file_provider),
            photoFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun launchCameraVideo() {
        val videoFile = File(
            requireContext().cacheDir,
            "video_${System.currentTimeMillis()}.mp4"
        )
        cameraVideoUri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().getString(R.string.file_provider),
            videoFile
        )
        cameraVideoLauncher.launch(cameraVideoUri!!)
    }

    private fun handleSelectedFile(file: File, mimeType: String) {
        val peerJid = viewModel.peerJid.value ?: return
        val attachmentType = mimeTypeToAttachmentType(mimeType)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            LoquaceXmppManager.uploadAndSendFile(
                toJid          = peerJid,
                file           = file,
                attachmentName = file.name,
                attachmentType = attachmentType,
                localPath      = file.absolutePath
            )
        }
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        val peerJid = viewModel.peerJid.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            for (uri in uris) {
                val file = uriToFile(uri) ?: continue
                val mimeType = requireContext().contentResolver.getType(uri)
                    ?: getMimeTypeFromFile(file) // Fallback to file extension
                val attachmentType = mimeTypeToAttachmentType(mimeType)

                launch(Dispatchers.IO) {
                    LoquaceXmppManager.uploadAndSendFile(
                        toJid          = peerJid,
                        file           = file,
                        attachmentName = file.name,
                        attachmentType = attachmentType,
                        localPath      = file.absolutePath
                    )
                }
            }
        }
    }

    private fun getMimeTypeFromFile(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "avi" -> "video/avi"
            "pdf" -> "application/pdf"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(uri)
            val tempFile = File(requireContext().cacheDir, fileName)
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            Log.e("$TAG Failed to convert URI to file: ${e.message}")
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "attachment_${System.currentTimeMillis()}"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
        return name
    }

    private fun mimeTypeToAttachmentType(mimeType: String): AttachmentType {
        return when {
            mimeType.startsWith("image/") -> AttachmentType.IMAGE
            mimeType.startsWith("video/") -> AttachmentType.VIDEO
            mimeType.startsWith("audio/") -> AttachmentType.AUDIO
            else -> AttachmentType.FILE
        }
    }

    private fun openMediaFullScreen(message: XmppMessage) {
        // If already cached locally, open directly
        if (!message.localPath.isNullOrEmpty()) {
            val file = File(message.localPath!!)
            if (file.exists()) {
                openFileDirectly(file, message)
                return
            }
        }

        // Otherwise download first
        val token = SessionManager(requireContext()).getToken() ?: ""
        val domain = SessionManager(requireContext()).getDomain() ?: ""

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fetchInProgress.value = true
            val bytes = withContext(Dispatchers.IO) {
                LoquaceMediaDownloader.downloadBytes(
                    url    = message.attachmentUrl!!,
                    token  = token,
                    domain = domain
                )
            }
            viewModel.fetchInProgress.value = false

            if (bytes != null) {
                val ext = message.attachmentName?.substringAfterLast(".") ?:
                if (message.isVideo) "mp4" else "jpg"
                val file = File(requireContext().cacheDir, "media_${message.id}.$ext")
                file.writeBytes(bytes)
                message.localPath = file.absolutePath
                openFileDirectly(file, message)
            }
        }
    }

    private fun openFileDirectly(file: File, message: XmppMessage) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().getString(R.string.file_provider),
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (message.isVideo) "video/*" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun openDocument(message: XmppMessage) {
        val token = SessionManager(requireContext()).getToken() ?: ""
        val domain = SessionManager(requireContext()).getDomain() ?: ""

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fetchInProgress.value = true
            val bytes = withContext(Dispatchers.IO) {
                LoquaceMediaDownloader.downloadBytes(
                    url    = message.attachmentUrl!!,
                    token  = token,
                    domain = domain
                )
            }
            viewModel.fetchInProgress.value = false

            if (bytes != null) {
                val fileName = message.attachmentName ?: "document_${message.id}"
                val file = File(requireContext().cacheDir, fileName)
                file.writeBytes(bytes)

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getString(R.string.file_provider),
                    file
                )
                val mimeType = requireContext().contentResolver.getType(uri) ?: "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("$TAG No app found to open document: ${e.message}")
                }
            } else {
                Log.e("$TAG Failed to download document for viewing")
            }
        }
    }

    private fun startVoiceRecording() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        voiceRecorder.startRecording()
        recordingStartTime = System.currentTimeMillis()

        binding.messageInput.visibility = View.GONE
        binding.attachButton.visibility = View.GONE
        binding.recordingArea.visibility = View.VISIBLE

        recordingTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                binding.recordingTimer.text = "$minutes:${seconds.toString().padStart(2, '0')}"
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun stopVoiceRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        binding.messageInput.visibility = View.VISIBLE
        binding.attachButton.visibility = View.VISIBLE
        binding.recordingArea.visibility = View.GONE

        val file = voiceRecorder.stopRecording()
        if (file != null && file.length() > 0) {
            val peerJid = viewModel.peerJid.value ?: return
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                LoquaceXmppManager.uploadAndSendFile(
                    toJid          = peerJid,
                    file           = file,
                    attachmentName = file.name,
                    attachmentType = AttachmentType.VOICE_NOTE,
                    localPath      = file.absolutePath
                )
            }
        } else {
            Log.w("$TAG Voice recording too short or failed")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMicButton() {
        binding.micButton.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startVoiceRecording()
                    true
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    stopVoiceRecording()
                    true
                }
                else -> false
            }
        }
    }

    private fun showMessageContextMenu(message: XmppMessage) {
        val options = arrayOf(
            getString(R.string.message_edit),
            getString(R.string.message_delete)
        )
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditMessageDialog(message)
                    1 -> deleteMessage(message)
                }
            }
            .show()
    }

    private fun deleteMessage(message: XmppMessage) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.message_delete))
            .setMessage(getString(R.string.message_delete_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteMessage(message)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditMessageDialog(message: XmppMessage) {
        val input = android.widget.EditText(requireContext()).apply {
            setText(message.body)
            setPadding(48, 32, 48, 32)
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.message_edit))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty() && newText != message.body) {
                    viewModel.editMessage(message, newText)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}