package presentation.settings_screen

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.cerebus.tokens.feature.tokens_feature.R
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.state.SaveState

class SelectColorDialogFragment : DialogFragment() {
    private val viewModel: SelectColorViewModel by viewModel()
    private var picker: ColorPickerView? = null
    private var rendering: Job? = null
    private var initialized = false

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val builder = ColorPickerDialog.Builder(requireContext())
        picker = builder.colorPickerView
        val diameter = resources.getDimensionPixelSize(R.dimen.picker_view_diameter)
        picker?.layoutParams?.let { it.height = diameter; it.width = diameter }
        return builder.setTitle(R.string.select_color)
            .setMessage(com.cerebus.tokens.core.ui.R.string.storage_loading)
            .setPositiveButton(R.string.select_button_text, DialogInterface.OnClickListener { _, _ -> })
            .setNegativeButton(com.cerebus.tokens.core.ui.R.string.cancel) { _, _ -> dismiss() }
            .attachAlphaSlideBar(false).attachBrightnessSlideBar(false).create()
    }

    override fun onStart() {
        super.onStart()
        val alert = requireDialog() as AlertDialog
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (viewModel.state.value.readError) viewModel.retryRead()
            else picker?.let { viewModel.save(it.color) }
        }
        // This DialogFragment has no Fragment View; bind collection to the visible dialog.
        rendering = lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state.save == SaveState.SAVED) { dismiss(); return@collect }
                if (!initialized && state.color != null) {
                    picker?.setInitialColor(state.color)
                    initialized = true
                }
                val saving = state.save == SaveState.SAVING
                isCancelable = !saving
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !saving
                alert.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !saving && (state.color != null || state.readError)
                picker?.isEnabled = !saving && state.color != null && !state.readError
                alert.setMessage(when {
                    state.readError -> getString(com.cerebus.tokens.core.ui.R.string.storage_read_error)
                    state.save == SaveState.ERROR -> getString(com.cerebus.tokens.core.ui.R.string.storage_save_error)
                    state.color == null -> getString(com.cerebus.tokens.core.ui.R.string.storage_loading)
                    else -> null
                })
            }
        }
    }

    override fun onStop() {
        rendering?.cancel()
        rendering = null
        super.onStop()
    }

    override fun onDestroyView() {
        picker = null
        initialized = false
        super.onDestroyView()
    }
}
