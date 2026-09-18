package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import com.cerebus.tokens.core.ui.setTokensContent
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.reinforcement_photo.R
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Transitional host: external activity contracts and native dismissal stay here until Navigation Compose. */
class AskForReinforcementImageDialog : DialogFragment() {
    private val viewModel: ChangePhotoViewModel by viewModel()
    private val gallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        updateOperation { viewModel.onGalleryResult(it?.toString()) }
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        updateOperation { viewModel.onCameraResult(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, DEFAULT_DIALOG_THEME)
        isCancelable = viewModel.state.value.cancellable
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.photo_compose_view
            setTokensContent {
                PhotoRoute(viewModel,
                    onCamera = { updateOperation(viewModel::takePhoto) },
                    onGallery = { updateOperation(viewModel::chooseGallery) },
                    onRetry = { updateOperation(viewModel::retry) },
                    onCancel = { if (viewModel.cancel()) dismiss() },
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.state) { state ->
            isCancelable = state.cancellable
            if (state.saved) {
                dismiss()
            } else {
                when (viewModel.consumeLaunch()) {
                    ImageSource.GALLERY -> launchSource {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    ImageSource.CAMERA -> launchSource { camera.launch(requireNotNull(state.captureUri).toUri()) }
                    null -> Unit
                }
            }
        }
    }

    private fun updateOperation(action: () -> Unit) {
        action()
        // Block native Back/outside cancellation before the next Compose frame.
        isCancelable = viewModel.state.value.cancellable
    }

    private fun launchSource(action: () -> Unit) {
        try { action() }
        catch (_: ActivityNotFoundException) { viewModel.sourceUnavailable() }
        catch (_: SecurityException) { viewModel.sourceUnavailable() }
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.cancel()
        super.onCancel(dialog)
    }

    private companion object { const val DEFAULT_DIALOG_THEME = 0 }
}
