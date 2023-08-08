package com.cerebus.tokens.settings_screen

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.R
import com.cerebus.tokens.databinding.FragmentSettingsBinding
import com.cerebus.tokens.tokens_screen.SelectTokenNumberAlert
import com.cerebus.tokens.tokens_screen.TokensNumberListener

class SettingsFragment: Fragment(R.layout.fragment_settings), TokensNumberListener {

    private val viewModel: SettingsViewModel by activityViewModels()
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
        changeColorLiveData.observe(viewLifecycleOwner) {
            if (it)
                SelectColorDialogFragment().show(requireActivity().supportFragmentManager, SelectColorDialogFragment.TAG)
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
        const val TAG = "AboutAppFragment"

        fun newInstance() = SettingsFragment()
    }
}
