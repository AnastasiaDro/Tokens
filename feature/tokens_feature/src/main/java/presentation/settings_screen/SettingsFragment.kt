package presentation.settings_screen

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.databinding.FragmentSettingsBinding
import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.SelectTokensNumberAlertData
import presentation.state.StorageFailure

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val viewModel: SettingsViewModel by viewModel()
    private val binding: FragmentSettingsBinding by viewBinding()
    private var rendering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        with(binding.settingsAppLayout) {
            changeTokensColorButton.setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_selectColorDialogFragment)
            }
            changeTokensNumberButton.setOnClickListener {
                val count = viewModel.state.value.tokens?.count ?: return@setOnClickListener
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToSelectTokenNumberAlert(
                    SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, count)))
            }
            animationSwitch.setOnCheckedChangeListener { _, value -> if (!rendering) viewModel.changeAnimation(value) }
            soundSwitch.setOnCheckedChangeListener { _, value -> if (!rendering) viewModel.changeSound(value) }
            reinforcementSwitch.setOnCheckedChangeListener { _, value -> if (!rendering) viewModel.changeReinforcement(value) }
            storageStatus.setOnClickListener { viewModel.retry() }
        }
        binding.aboutAppLayout.youtubeLinkTextView.movementMethod = LinkMovementMethod.getInstance()
        binding.aboutAppLayout.donateLinkTextView.movementMethod = LinkMovementMethod.getInstance()
        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.state, ::render)
    }

    private fun render(state: SettingsUiState) = with(binding.settingsAppLayout) {
        rendering = true
        val enabled = !state.loading && !state.saving && state.tokens != null && state.error != StorageFailure.READ
        listOf(changeTokensColorButton, changeTokensNumberButton, animationSwitch, soundSwitch, reinforcementSwitch)
            .forEach { it.isEnabled = enabled }
        currentTokensNumberTextView.text = state.tokens?.count?.toString().orEmpty()
        state.tokens?.let { tokenColorPreview.setBackgroundColor(it.color) }
        animationSwitch.isChecked = state.effects?.animation ?: false
        soundSwitch.isChecked = state.effects?.sound ?: false
        // Preserve the existing camera gate until the separately planned photo stage.
        reinforcementSwitch.isChecked = state.reinforcement?.enabled == true &&
            requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        storageStatus.visibility = if (state.loading || state.error != null) View.VISIBLE else View.GONE
        storageStatus.isEnabled = state.error != null
        storageStatus.setText(when (state.error) {
            StorageFailure.READ -> com.cerebus.tokens.core.ui.R.string.storage_read_error
            StorageFailure.WRITE -> com.cerebus.tokens.core.ui.R.string.storage_write_error
            null -> com.cerebus.tokens.core.ui.R.string.storage_loading
        })
        rendering = false
    }
}
