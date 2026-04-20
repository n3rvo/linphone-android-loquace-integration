package org.linphone.ui.main.help.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.LoquaceAboutFragmentBinding
import org.linphone.ui.main.fragment.GenericMainFragment
import org.linphone.ui.main.help.viewmodel.HelpViewModel
import androidx.core.net.toUri

@UiThread
class LoquaceAboutFragment : GenericMainFragment() {

    companion object {
        private const val TAG = "[Loquace About Fragment]"
    }

    private lateinit var binding: LoquaceAboutFragmentBinding
    private lateinit var viewModel: HelpViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LoquaceAboutFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HelpViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.setBackClickListener {
            goBack()
        }

        binding.privacyTitle.setOnClickListener {
            openUrl(getString(R.string.about_privacy_policy_url))
        }
        binding.privacySubtitle.setOnClickListener {
            openUrl(getString(R.string.about_privacy_policy_url))
        }
        binding.privacyIcon.setOnClickListener {
            openUrl(getString(R.string.about_privacy_policy_url))
        }

        binding.licensesTitle.setOnClickListener {
            openUrl(getString(R.string.website_open_source_licences_usage_url))
        }
        binding.licensesSubtitle.setOnClickListener {
            openUrl(getString(R.string.website_open_source_licences_usage_url))
        }
        binding.licensesIcon.setOnClickListener {
            openUrl(getString(R.string.website_open_source_licences_usage_url))
        }

        binding.linphoneTitle.setOnClickListener {
            openUrl(getString(R.string.about_linphone_url))
        }
        binding.linphoneSubtitle.setOnClickListener {
            openUrl(getString(R.string.about_linphone_url))
        }
        binding.linphoneIcon.setOnClickListener {
            openUrl(getString(R.string.about_linphone_url))
        }

        binding.gplTitle.setOnClickListener {
            openUrl(getString(R.string.about_gpl_url))
        }
        binding.gplSubtitle.setOnClickListener {
            openUrl(getString(R.string.about_gpl_url))
        }
        binding.gplIcon.setOnClickListener {
            openUrl(getString(R.string.about_gpl_url))
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("$TAG Failed to open URL [$url]: $e")
        }
    }
}