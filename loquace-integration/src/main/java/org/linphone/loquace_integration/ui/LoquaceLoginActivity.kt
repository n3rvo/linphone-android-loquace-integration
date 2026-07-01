package org.linphone.loquace_integration.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.linphone.loquace_integration.databinding.ActivityLoginBinding
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.viewmodel.LoginState
import org.linphone.loquace_integration.viewmodel.LoquaceLoginViewModel
import org.linphone.loquace_integration.viewmodel.LoquaceLoginViewModelFactory

class LoquaceLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoquaceLoginViewModel by viewModels {
        LoquaceLoginViewModelFactory(
            context        = applicationContext,
            sessionManager = SessionManager(this)
        )
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val domain   = binding.inputDomain.text.toString().trim()
            val username = binding.inputUsername.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            if (domain.isEmpty() || username.isEmpty() || password.isEmpty()) {
                binding.txtError.visibility = View.VISIBLE
                binding.txtError.text = getString(org.linphone.loquace_integration.R.string.login_error_empty_fields)
                return@setOnClickListener
            }

            viewModel.login(domain, username, password)
        }

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is LoginState.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.loading.visibility = View.VISIBLE
                        binding.txtError.visibility = View.GONE
                    }
                    is LoginState.Success -> {
                        binding.loading.visibility = View.GONE
                        val intent = Intent().apply {
                            putExtra(SHOW_PERMISSIONS, true)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                    is LoginState.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.loading.visibility = View.GONE
                        binding.txtError.visibility = View.VISIBLE
                        binding.txtError.text = state.message
                    }
                    is LoginState.Idle -> {
                        binding.btnLogin.isEnabled = true
                        binding.loading.visibility = View.GONE
                    }
                }
            }
        }
    }

    companion object {
        const val SHOW_PERMISSIONS = "show_permissions"
    }
}