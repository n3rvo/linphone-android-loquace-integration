package org.linphone.loquace_integration.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import org.linphone.loquace_integration.storage.SessionManager
import org.linphone.loquace_integration.viewmodel.LoginState
import org.linphone.loquace_integration.viewmodel.LoquaceLoginViewModel
import org.linphone.loquace_integration.viewmodel.LoquaceLoginViewModelFactory
import kotlinx.coroutines.launch
import org.linphone.loquace_integration.databinding.ActivityLoginBinding

class LoquaceLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoquaceLoginViewModel by viewModels {
        LoquaceLoginViewModelFactory(
            context        = applicationContext,
            sessionManager = SessionManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                domain   = binding.inputDomain.text.toString().trim(),
                username = binding.inputUsername.text.toString().trim(),
                password = binding.inputPassword.text.toString().trim()
            )
        }

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is LoginState.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.txtError.visibility = View.GONE
                    }
                    is LoginState.Success -> {
                        // TODO: navigate to main screen
                    }
                    is LoginState.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.txtError.visibility = View.VISIBLE
                        binding.txtError.text = state.message
                    }
                    is LoginState.Idle -> {
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }
}