package org.linphone.loquace_integration.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.linphone.loquace_integration.storage.SessionManager

class LoquaceLoginViewModelFactory(
    private val context: Context,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LoquaceLoginViewModel(context, sessionManager) as T
    }
}