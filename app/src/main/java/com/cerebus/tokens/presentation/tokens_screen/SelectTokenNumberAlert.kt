package com.cerebus.tokens.presentation.tokens_screen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.R
import com.cerebus.tokens.databinding.AlertSelectTokensNumberBinding
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
class SelectTokenNumberAlert(private val currentTokensNumber: Int, private val minTokensValue: Int, private val maxTokensValue: Int, private val tokensNumberListener: TokensNumberListener): DialogFragment(
    R.layout.alert_select_tokens_number
) {

    private var newTokensNumber = 1
    private val viewBinding: AlertSelectTokensNumberBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNumPicker()
        initButtons()
    }

    private fun initNumPicker() {
        with(viewBinding.tokensNumPicker) {
            minValue = minTokensValue
            maxValue = maxTokensValue
            value = currentTokensNumber
            setOnValueChangedListener { _, _, newVal ->
                newTokensNumber = newVal
            }
        }
    }

    private fun initButtons() {
        with(viewBinding) {
            okBtn.setOnClickListener {
                tokensNumberListener.changeTokensNumber(newTokensNumber)
                dismiss()
            }
            cancelBtn.setOnClickListener { dismiss() }
        }
    }

    companion object {
        const val TAG = "SelectTokenNumberAlert"
    }

}