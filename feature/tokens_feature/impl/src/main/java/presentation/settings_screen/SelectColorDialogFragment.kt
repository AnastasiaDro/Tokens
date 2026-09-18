package presentation.settings_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import com.cerebus.tokens.core.ui.setTokensContent
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.feature.tokens_feature.R
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.state.SaveState

class SelectColorDialogFragment : DialogFragment() {
    private val viewModel: SelectColorViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, DEFAULT_DIALOG_THEME)
        isCancelable = viewModel.state.value.cancellable
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.color_compose_view
            setTokensContent {
                SelectColorRoute(
                    viewModel = viewModel,
                    onConfirm = {
                        viewModel.save()
                        // Block native Back/outside dismissal before the next frame.
                        isCancelable = viewModel.state.value.cancellable
                    },
                    onCancel = { if (viewModel.state.value.cancellable) dismiss() },
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.state) { state ->
            isCancelable = state.cancellable
            if (state.save == SaveState.SAVED) dismiss()
        }
    }

    private companion object { const val DEFAULT_DIALOG_THEME = 0 }
}
