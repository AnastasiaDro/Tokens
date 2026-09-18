package presentation.tokens_screen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.databinding.AlertSelectTokensNumberBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.state.SaveState

class SelectTokenNumberAlert : DialogFragment(R.layout.alert_select_tokens_number) {
    private val binding: AlertSelectTokensNumberBinding by viewBinding()
    private val args: SelectTokenNumberAlertArgs by navArgs()
    private val viewModel: SelectTokensNumberViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding) {
            tokensNumPicker.minValue = args.tokensNumberData.minTokensNum
            tokensNumPicker.maxValue = args.tokensNumberData.maxTokensNum
            if (savedInstanceState == null) tokensNumPicker.value = args.tokensNumberData.currentTokensNum
            okBtn.setOnClickListener {
                tokensNumPicker.clearFocus()
                viewModel.changeTokensNum(tokensNumPicker.value)
            }
            cancelBtn.setOnClickListener { dismiss() }
        }
        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.state) { state ->
            if (state == SaveState.SAVED) dismiss()
            with(binding) {
                val saving = state == SaveState.SAVING
                isCancelable = !saving
                okBtn.isEnabled = !saving
                cancelBtn.isEnabled = !saving
                tokensNumPicker.isEnabled = !saving
                storageError.visibility = if (state == SaveState.ERROR) View.VISIBLE else View.GONE
            }
        }
    }
}
