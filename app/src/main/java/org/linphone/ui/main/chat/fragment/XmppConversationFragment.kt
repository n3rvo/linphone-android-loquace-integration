package org.linphone.ui.main.chat.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.LoquaceChatConversationFragmentBinding
import org.linphone.loquace_integration.xmpp.AttachmentType
import org.linphone.loquace_integration.xmpp.LoquaceXmppManager
import org.linphone.loquace_integration.xmpp.XmppHttpUploadManager
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

    private val args: XmppConversationFragmentArgs by navArgs()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedFiles(uris)
        }
    }

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
                    handleSelectedFiles(listOf(uri))
                } else {
                    Log.e("$TAG Camera file is empty")
                }
            }
        }
    }

    private var cameraImageUri: Uri? = null

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

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d(TAG, "Messages updated: ${messages.size} items")
            adapter.submitList(messages) {
                binding.messagesList.scrollToPosition(adapter.itemCount - 1)
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
    }

    override fun goBack(): Boolean {
        sharedViewModel.closeSlidingPaneEvent.value = Event(true)
        return true
    }

    private fun showAttachmentPicker() {
        val options = arrayOf("Image", "Video", "File", "Camera")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Attach")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> filePickerLauncher.launch("image/*")
                    1 -> filePickerLauncher.launch("video/*")
                    2 -> filePickerLauncher.launch("application/*")
                    3 -> launchCamera()
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

    private fun handleSelectedFiles(uris: List<Uri>) {
        val peerJid = viewModel.peerJid.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            for (uri in uris) {
                val file = uriToFile(uri) ?: continue
                val mimeType = requireContext().contentResolver.getType(uri) ?: "application/octet-stream"
                val attachmentType = mimeTypeToAttachmentType(mimeType)

                viewModel.fetchInProgress.value = true

                val result = withContext(Dispatchers.IO) {
                    LoquaceXmppManager.uploadAndSendFile(
                        toJid          = peerJid,
                        file           = file,
                        attachmentName = file.name,
                        attachmentType = attachmentType
                    )
                }

                if (result == null) {
                    Log.e("$TAG Failed to upload and send file ${file.name}")
                }

                viewModel.fetchInProgress.value = false
            }
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
}