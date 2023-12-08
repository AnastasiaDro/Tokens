package presentation.tokens_screen

import SelectTokensNumberAlertData.Companion.CURRENT_TOKENS_NUMBER_RESULT_KEY
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.setNavigationResult
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.databinding.AlertSelectTokensNumberBinding
import presentation.settings_screen.SettingsFragment

/**
 * [SelectTokenNumberAlert] - a dialog for selecting number of tokens
 * Can be called by the [TokensFragment] options menu and from the [SettingsFragment]
 * a typical parser of values is [TokensNumberListener]
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
        Log.d(TAG, " was initialized")
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
            Log.d(TAG, " was closed")
        }
    }

    companion object {
        const val TAG = "SelectTokenNumberAlert"
    }
}