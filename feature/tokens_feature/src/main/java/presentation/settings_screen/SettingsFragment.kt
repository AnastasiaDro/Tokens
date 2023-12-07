package presentation.settings_screen

import SelectTokensNumberAlertData
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.getNavigationResultLiveData
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.databinding.FragmentSettingsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import presentation.tokens_screen.TokenView
import presentation.tokens_screen.TokensNumberListener

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

    private val viewModel: SettingsViewModel by activityViewModel<SettingsViewModel>()
    private val viewBinding: FragmentSettingsBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(viewBinding.settingsAppLayout) {
            changeTokensColorButton.setOnClickListener { viewModel.askForChangeTokensColor() }
            changeTokensNumberButton.setOnClickListener {
                viewModel.askChangeTokensNumber()
            }
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
        subscribeToNavigationResultLiveData()
        subscribeToViewModel()
    }

    private fun subscribeToViewModel() = with(viewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                changeColorFlow.collectLatest {
                    findNavController().navigate(R.id.action_settingsFragment_to_selectColorDialogFragment)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                selectTokensNumberFlow.collectLatest {
                    findNavController().navigate(
                        getTokensNumberAlertNavAction(
                            minTokensNum = viewModel.getMinTokensNum(),
                            maxTokensNum = viewModel.getMaxTokensNum(),
                            currentTokensNum = viewModel.getTokensNum()
                        )
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

    override fun getTokensNumberAlertNavAction(
        minTokensNum: Int,
        maxTokensNum: Int,
        currentTokensNum: Int
    ): NavDirections {
        return SettingsFragmentDirections.actionSettingsFragmentToSelectTokenNumberAlert(
            SelectTokensNumberAlertData(minTokensNum, maxTokensNum, currentTokensNum)
        )
    }

    companion object {
        const val TAG = "SettingsFragment"
    }

    override fun subscribeToNavigationResultLiveData() {
        val result = getNavigationResultLiveData<Int>(SelectTokensNumberAlertData.CURRENT_TOKENS_NUMBER_RESULT_KEY)
        result?.observe(viewLifecycleOwner) {newNum ->
            viewModel.changeTokensNum(newNum)
        }
    }
}
