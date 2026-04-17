package org.linphone.ui.main.settings.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.linphone.R
import org.linphone.databinding.SettingsAdvancedFragmentBinding
import org.linphone.ui.GenericActivity
import org.linphone.ui.main.fragment.GenericMainFragment
import org.linphone.ui.main.settings.LoquacePermissionsAdapter
import org.linphone.ui.main.settings.PermissionItem
import org.linphone.ui.main.settings.viewmodel.SettingsViewModel
import org.linphone.utils.Event

@UiThread
class SettingsAdvancedFragment : GenericMainFragment() {
    private lateinit var binding: SettingsAdvancedFragmentBinding
    private lateinit var viewModel: SettingsViewModel
    private var permissionsAdapter: LoquacePermissionsAdapter? = null
    private var permissionsPanelOpen = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Refresh permissions list after request
        setupPermissionsList()
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        setupPermissionsList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsAdvancedFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            goBack()
        }

        binding.setAndroidSettingsClickListener {
            (requireActivity() as GenericActivity).goToAndroidPermissionSettings()
        }

        viewModel.keepAliveServiceSettingChangedEvent.observe(viewLifecycleOwner) {
            it.consume {
                sharedViewModel.refreshDrawerMenuQuitButtonEvent.postValue(Event(true))
            }
        }

        // Permissions accordion
        binding.permissionsTitle.setOnClickListener {
            permissionsPanelOpen = !permissionsPanelOpen
            binding.permissionsList.visibility =
                if (permissionsPanelOpen) View.VISIBLE else View.GONE
            binding.permissionsTitle.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (permissionsPanelOpen) R.drawable.caret_up else R.drawable.caret_down,
                0
            )
            if (permissionsPanelOpen) setupPermissionsList()
        }

        startPostponedEnterTransition()
    }

    override fun onResume() {
        super.onResume()
        if (permissionsPanelOpen) setupPermissionsList()
    }

    private fun setupPermissionsList() {
        val permissions = listOf(
            PermissionItem(
                name = getString(R.string.permission_record_audio),
                isGranted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED,
                onClickRequest = {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            ),
            PermissionItem(
                name = getString(R.string.permission_camera),
                isGranted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED,
                onClickRequest = {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            ),
            PermissionItem(
                name = getString(R.string.permission_read_contacts),
                isGranted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED,
                onClickRequest = {
                    requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            ),
            PermissionItem(
                name = getString(R.string.permission_bluetooth),
                isGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                } else true,
                onClickRequest = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }
            ),
            PermissionItem(
                name = getString(R.string.permission_post_notifications),
                isGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true,
                onClickRequest = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        )

        permissionsAdapter = LoquacePermissionsAdapter(permissions)
        binding.permissionsList.layoutManager = LinearLayoutManager(requireContext())
        binding.permissionsList.adapter = permissionsAdapter
    }
}