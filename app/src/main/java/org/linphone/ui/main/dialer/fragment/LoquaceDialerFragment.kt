package org.linphone.ui.main.dialer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.LoquaceDialerFragmentBinding
import org.linphone.ui.main.dialer.viewmodel.LoquaceDialerViewModel
import org.linphone.ui.main.fragment.AbstractMainFragment

@UiThread
class LoquaceDialerFragment : AbstractMainFragment() {

    companion object {
        private const val TAG = "[Loquace Dialer Fragment]"
    }

    private lateinit var binding: LoquaceDialerFragmentBinding
    private lateinit var viewModel: LoquaceDialerViewModel

    override fun onDefaultAccountChanged() {
        Log.i("$TAG Default account changed")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.loquace_dialer_fragment,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[LoquaceDialerViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        observeToastEvents(viewModel)

        viewModel.title.value = getString(R.string.bottom_navigation_dialer_label)
        setViewModel(viewModel)
        initViews(
            binding.topBar,
            binding.bottomNavBar,
            R.id.loquaceDialerFragment
        )

        Log.i("$TAG Fragment created")
    }
}