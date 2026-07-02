/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.main.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.DrawerMenuBinding
import org.linphone.ui.assistant.AssistantActivity
import org.linphone.ui.main.MainActivity
import org.linphone.ui.main.settings.fragment.AccountProfileFragmentDirections
import org.linphone.ui.main.viewmodel.DrawerMenuViewModel
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.ui.main.viewmodel.LoquaceDrawerMenuViewModel
import org.linphone.utils.setLoquacePresenceRing
import java.util.Locale

@UiThread
class DrawerMenuFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Drawer Menu Fragment]"
    }

    private lateinit var binding: DrawerMenuBinding

    private lateinit var viewModel: DrawerMenuViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DrawerMenuBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = requireActivity().run {
            ViewModelProvider(this)[DrawerMenuViewModel::class.java]
        }
        val loquaceViewModel = ViewModelProvider(requireActivity())[LoquaceDrawerMenuViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.loquaceViewModel = loquaceViewModel
        observeToastEvents(viewModel)

        // Fetch data when drawer opens
        loquaceViewModel.fetchData(requireContext())

        // Close button
        viewModel.closeDrawerEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as MainActivity).closeDrawerMenu()
            }
        }

        viewModel.openAccountProfileEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                val navController = (requireActivity() as MainActivity).findNavController()
                val action = AccountProfileFragmentDirections.actionGlobalAccountProfileFragment(
                    model.identity
                )
                navController.navigate(action)
                (requireActivity() as MainActivity).closeDrawerMenu()
            }
        }

        sharedViewModel.refreshDrawerMenuAccountsListEvent.observe(viewLifecycleOwner) {
            it.consume { recreate ->
                if (recreate) {
                    viewModel.updateAccountsList()
                } else {
                    viewModel.refreshAccountsNotificationsCount()
                }
            }
        }

        // Accordion toggle for incoming calls
        var callsPanelOpen = false
        binding.incomingCallsRow.setOnClickListener {
            callsPanelOpen = !callsPanelOpen
            binding.incomingCallsPanel.visibility = if (callsPanelOpen) View.VISIBLE else View.GONE
            binding.incomingCallsRow.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.phone, 0,
                if (callsPanelOpen) R.drawable.caret_up else R.drawable.caret_down,
                0
            )
        }

        // Accordion toggle for presence
        var presencePanelOpen = false
        binding.presenceRow.setOnClickListener {
            presencePanelOpen = !presencePanelOpen
            binding.presencePanel.visibility = if (presencePanelOpen) View.VISIBLE else View.GONE
            binding.presenceRow.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.user_circle, 0,
                if (presencePanelOpen) R.drawable.caret_up else R.drawable.caret_down,
                0
            )
        }

        // Settings
        binding.settings.setOnClickListener {
            val navController = (requireActivity() as MainActivity).findNavController()
            navController.navigate(R.id.action_global_settingsFragment)
            (requireActivity() as MainActivity).closeDrawerMenu()
        }

        // About
        binding.about.setOnClickListener {
            val navController = (requireActivity() as MainActivity).findNavController()
            navController.navigate(R.id.action_global_loquaceAboutFragment)
            (requireActivity() as MainActivity).closeDrawerMenu()
        }

        // Accordion toggle for presence
        var languagePanelOpen = false
        binding.languageRow.setOnClickListener {
            languagePanelOpen = !languagePanelOpen
            binding.languagePanel.visibility = if (languagePanelOpen) View.VISIBLE else View.GONE
            binding.languageRow.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.globe_hemisphere_west, 0,
                if (languagePanelOpen) R.drawable.caret_up else R.drawable.caret_down,
                0
            )
        }

        // Presence spinner setup
        val statusOptions = listOf("ONLINE", "AWAY", "BUSY", "OFFLINE")
        val statusLabels = listOf(
            getString(R.string.drawer_status_online),
            getString(R.string.drawer_status_away),
            getString(R.string.drawer_status_busy),
            getString(R.string.drawer_status_offline)
        )
        val statusColors = listOf(
            R.color.green_success_500,  // ONLINE
            R.color.orange_warning_600, // AWAY
            R.color.red_danger_500,     // BUSY
            R.color.gray_400            // OFFLINE — use an existing grey color
        )

        val spinnerAdapter = object : android.widget.ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            statusLabels
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? android.widget.TextView)?.setTextColor(
                    ContextCompat.getColor(requireContext(), statusColors[position])
                )
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? android.widget.TextView)?.setTextColor(
                    ContextCompat.getColor(requireContext(), statusColors[position])
                )
                return view
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.presenceStatusSpinner.adapter = spinnerAdapter

        loquaceViewModel.presenceStatus.observe(viewLifecycleOwner) { status ->
            val index = statusOptions.indexOf(status)
            if (index >= 0) binding.presenceStatusSpinner.setSelection(index)
        }

        loquaceViewModel.presenceMessage.observe(viewLifecycleOwner) { message ->
            binding.presenceMessageInput.setText(message)
        }

        // Submit presence
        binding.submitPresenceButton.setOnClickListener {
            val selectedStatus = statusOptions[binding.presenceStatusSpinner.selectedItemPosition]
            val message = binding.presenceMessageInput.text?.toString() ?: ""
            loquaceViewModel.presenceStatus.value = selectedStatus
            loquaceViewModel.presenceMessage.value = message
            loquaceViewModel.submitPresence(requireContext())
        }

        loquaceViewModel.presenceStatus.observe(viewLifecycleOwner) { status ->
            val index = statusOptions.indexOf(status)
            if (index >= 0) binding.presenceStatusSpinner.setSelection(index)

            // Update drawer account avatar ring
            val accountsLinearLayout = binding.accountsScroll
                .getChildAt(0) as? android.widget.LinearLayout ?: return@observe
            val firstAccount = accountsLinearLayout.getChildAt(0) ?: return@observe
            firstAccount.findViewById<android.widget.ImageView>(R.id.presence_ring)
                ?.setLoquacePresenceRing(status)
        }

        // Calls switches - just update ViewModel, don't submit yet
        binding.mobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            loquaceViewModel.mobileEnabled.value = isChecked
        }
        binding.browserSwitch.setOnCheckedChangeListener { _, isChecked ->
            loquaceViewModel.browserEnabled.value = isChecked
        }
        binding.phoneSwitch.setOnCheckedChangeListener { _, isChecked ->
            loquaceViewModel.phoneEnabled.value = isChecked
        }

        // Submit calls
        binding.submitCallsButton.setOnClickListener {
            loquaceViewModel.submitCallsSettings(requireContext())
        }

        // Language spinner set up
        val languages = listOf("English", "Italiano")
        val languageCodes = listOf("en", "it")
        val currentLanguage = requireContext()
            .getSharedPreferences("loquace_preferences", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"

        val languageSpinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            languages
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.languageSpinner.adapter = languageSpinnerAdapter
        binding.languageSpinner.setSelection(languageCodes.indexOf(currentLanguage), false)

        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = languageCodes[position]
                if (selectedLanguage != currentLanguage) {
                    loquaceViewModel.setLanguage(selectedLanguage)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loquaceViewModel.languageChangedEvent.observe(viewLifecycleOwner) {
            it.consume { language ->
                applyLanguage(language, requireActivity())
            }
        }

        // Logout
        binding.logoutButton.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.drawer_logout)
                .setMessage(R.string.logout_confirmation_message)
                .setPositiveButton(R.string.drawer_logout) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Stop XMPP service
                        requireActivity().stopService(
                            Intent(requireContext(),
                                org.linphone.loquace_integration.xmpp.XmppConnectionService::class.java)
                        )

                        // Run logout
                        withContext(Dispatchers.IO) {
                            org.linphone.loquace_integration.network.LoquaceLogoutManager.logout(
                                requireContext()
                            )
                        }

                        // Navigate to login with clean stack
                        val intent = Intent(
                            requireContext(),
                            org.linphone.loquace_integration.ui.LoquaceLoginActivity::class.java
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
            (requireActivity() as MainActivity).closeDrawerMenu()
        }
    }

    fun applyLanguage(language: String, activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ — use AppCompatDelegate (no restart needed)
            val localeList = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(localeList)
        } else {
            // Pre-API 33 — requires activity restart
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = Configuration(activity.resources.configuration)
            config.setLocale(locale)
            activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
            activity.recreate()
        }
    }
}
