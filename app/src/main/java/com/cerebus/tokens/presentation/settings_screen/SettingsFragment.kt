package com.cerebus.tokens.presentation.settings_screen

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.R
import com.cerebus.tokens.databinding.FragmentSettingsBinding
import com.cerebus.tokens.presentation.tokens_screen.SelectTokenNumberAlert
import com.cerebus.tokens.presentation.tokens_screen.TokenView
import com.cerebus.tokens.presentation.tokens_screen.TokensFragment
import com.cerebus.tokens.presentation.tokens_screen.TokensNumberListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * [SettingsFragment] - a fragment for changing settings
 * A user can change
 * - number of tokens
 * - tokens color
 * - win animation on/off
 * - win sound on/off
 * @see TokenView
 *
 * @author Anastasia Drogunova
 * @since 23.05.2023
 */
class SettingsFragment: Fragment(R.layout.fragment_settings), TokensNumberListener {

    private val viewModel = ViewModelProvider(this, SettingsViewModelFactory(requireContext())).get(SettingsViewModel::class.java)
    private val viewBinding: FragmentSettingsBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(viewBinding.settingsAppLayout) {
            changeTokensColorButton.setOnClickListener { viewModel.askForChangeTokensColor() }
            changeTokensNumberButton.setOnClickListener { SelectTokenNumberAlert(viewModel.getTokensNum(), viewModel.getMinTokensNumber(), viewModel.getMaxTokensNumber(), this@SettingsFragment).show(requireActivity().supportFragmentManager,
                SelectTokenNumberAlert.TAG) }
            currentTokensNumberTextView.text = viewModel.getTokensNum().toString()
            animationSwitch.isChecked = viewModel.getIsAnimation()
            soundSwitch.isChecked = viewModel.getIsSound()
            animationSwitch.setOnCheckedChangeListener { _, isChecked -> viewModel.changeAnimation(isChecked) }
            soundSwitch.setOnCheckedChangeListener { _, isChecked -> viewModel.changeSound(isChecked) }
        }
        with(viewBinding.aboutAppLayout) {
            youtubeLinkTextView.movementMethod = LinkMovementMethod.getInstance()
            donateLinkTextView.movementMethod = LinkMovementMethod.getInstance()
        }
        Log.d(TAG, "Views were initialized")
        subscribeToViewModel()
    }

    override fun changeTokensNumber(newNumber: Int) {
        viewModel.changeTokensNum(newNumber)
    }

    private fun subscribeToViewModel() = with(viewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                changeColorFlow.collectLatest {
                    SelectColorDialogFragment().show(requireActivity().supportFragmentManager,
                        SelectColorDialogFragment.TAG
                    )
                }
            }
        }
        colorLiveData.observe(viewLifecycleOwner) { newColor ->
            viewBinding.settingsAppLayout.colorPreviewCard.circleColorExample.setBackgroundColor(newColor)
        }
        changedTokensNumLiveData.observe(viewLifecycleOwner) {
            if (it) viewBinding.settingsAppLayout.currentTokensNumberTextView.text = viewModel.getTokensNum().toString()
        }
        Log.d(TAG, "subscribed to viewModel's data")
    }

    companion object {
        const val TAG = "SettingsFragment"

        fun newInstance() = SettingsFragment()
    }
}
