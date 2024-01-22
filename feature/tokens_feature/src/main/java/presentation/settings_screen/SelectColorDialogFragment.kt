package presentation.settings_screen

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.cerebus.tokens.feature.tokens_feature.R
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * [SelectColorDialogFragment] - a dialog with [ColorPickerView]
 * for setting checked tokens color
 *
 * @see SettingsFragment
 * @see SettingsViewModel
 *
 * @author Anastasia Drogunova
 * @since 07.06.2023
 */
class SelectColorDialogFragment: DialogFragment() {

    private val viewModel: SettingsViewModel by activityViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {

        /**
         * Hardcoded layout params because I can't find a way to set a colorPickerView's listener in my custom dialog
         * ... May be later ...
         **/

        val dialog = ColorPickerDialog.Builder(requireContext())
            .apply {
                val layoutParams = colorPickerView.layoutParams
                val diameter = context.resources.getDimensionPixelSize(R.dimen.picker_view_diameter)
                layoutParams?.height = diameter
                layoutParams?.width = diameter
                colorPickerView.rootView.layoutParams = layoutParams
            }
                .setTitle(R.string.select_color)
                .setPreferenceName(TAG)
                .setPositiveButton(R.string.select_button_text, object : ColorEnvelopeListener {
                    override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                        viewModel.changeTokensColor(envelope.color)
                    }
                })
                .setNegativeButton(com.cerebus.tokens.core.ui.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(false)
                .create()
        Log.d(TAG, "dialog was created")
        return dialog
    }

    companion object {
        const val TAG = "SelectColorDialogFragment"
    }
}