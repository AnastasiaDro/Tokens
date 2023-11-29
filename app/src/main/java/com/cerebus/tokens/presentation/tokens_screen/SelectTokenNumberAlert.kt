package com.cerebus.tokens.presentation.tokens_screen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import androidx.navigation.fragment.navArgs
import com.cerebus.tokens.R
import com.cerebus.tokens.databinding.AlertSelectTokensNumberBinding
import com.cerebus.tokens.presentation.SelectTokensNumberAlertData
import com.cerebus.tokens.presentation.SelectTokensNumberAlertData.Companion.CURRENT_TOKENS_NUMBER_RESULT_KEY
import com.cerebus.tokens.presentation.getNavigationResultLiveData
import com.cerebus.tokens.presentation.setNavigationResult
import com.cerebus.tokens.presentation.settings_screen.SettingsFragment

/**
 * [SelectTokenNumberAlert] - a dialog for selecting number of tokens
 * Can be called by the [TokensFragment] options menu and from the [SettingsFragment]
 * a typical parser of values is [TokensNumberListener]
 *
 * @param minTokensValue - a minimum available number of tokens for collect
 * @param maxTokensValue - a maximum number of tokens for collect
 *
 *
 * @see TokensFragment
 * @see SettingsFragment
 * @see TokensNumberListener
 *
 * @author Anastasia Drogunova
 * @since 25.05.2023
 */
class SelectTokenNumberAlert: DialogFragment(R.layout.alert_select_tokens_number)
{
    private var newTokensNumber = 1
    private val viewBinding: AlertSelectTokensNumberBinding by viewBinding()
    private val navArgs: SelectTokenNumberAlertArgs by navArgs()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNumPicker()
        initButtons()
    }


    private fun initNumPicker() {
        with(viewBinding.tokensNumPicker) {
            minValue = navArgs.tokensNumberData.minTokensNum
            maxValue = navArgs.tokensNumberData.maxTokensNum
            value = navArgs.tokensNumberData.currentTokensNum
            setOnValueChangedListener { _, _, newVal ->
                newTokensNumber = newVal
            }
        }
    }

    private fun initButtons() {
        with(viewBinding) {
            okBtn.setOnClickListener {
                setNavigationResult(tokensNumPicker.value, CURRENT_TOKENS_NUMBER_RESULT_KEY)
                dismiss()
            }
            cancelBtn.setOnClickListener { dismiss() }
        }
    }

    companion object {
        const val TAG = "SelectTokenNumberAlert"
    }
}