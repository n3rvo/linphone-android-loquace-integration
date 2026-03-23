package org.linphone.ui.main.chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.LoquaceChatConversationFragmentBinding
import org.linphone.ui.main.chat.adapter.XmppMessagesAdapter
import org.linphone.ui.main.chat.viewmodel.XmppConversationViewModel
import org.linphone.ui.main.fragment.SlidingPaneChildFragment
import org.linphone.utils.Event

@UiThread
class XmppConversationFragment : SlidingPaneChildFragment() {

    companion object {
        private const val TAG = "[Xmpp Conversation Fragment]"
    }

    private lateinit var binding: LoquaceChatConversationFragmentBinding
    private lateinit var viewModel: XmppConversationViewModel
    private lateinit var adapter: XmppMessagesAdapter

    private val args: XmppConversationFragmentArgs by navArgs()

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
    }

    override fun goBack(): Boolean {
        sharedViewModel.closeSlidingPaneEvent.value = Event(true)
        return true
    }
}