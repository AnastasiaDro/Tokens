package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.setPhotoImage
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.reinforcement_photo.R
import com.cerebus.tokens.reinforcement_photo.databinding.DialogAskForReinforcementImageBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class AskForReinforcementImageDialog : DialogFragment(R.layout.dialog_ask_for_reinforcement_image) {
    private val binding: DialogAskForReinforcementImageBinding by viewBinding()
    private val viewModel: ChangePhotoViewModel by viewModel()

    private val gallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        viewModel.onGalleryResult(it?.toString())
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        viewModel.onCameraResult(it)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cancelButton.setOnClickListener { if (viewModel.cancel()) dismiss() }
        binding.makePhotoButton.setOnClickListener { viewModel.takePhoto() }
        binding.getFromGalleryButton.setOnClickListener { viewModel.chooseGallery() }
        binding.storageStatus.setOnClickListener { viewModel.retry() }
        var renderedUri: String? = null
        var initialized = false
        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.state) { state ->
            if (state.saved) {
                dismiss()
            } else {
                isCancelable = !state.loading && !state.saving
                binding.cancelButton.isEnabled = isCancelable
                binding.makePhotoButton.isEnabled = state.canSelect
                binding.getFromGalleryButton.isEnabled = state.canSelect
                binding.storageStatus.visibility = if (state.loading || state.error != null) View.VISIBLE else View.GONE
                binding.storageStatus.isEnabled = state.error != null && !state.saving
                binding.storageStatus.setText(when (state.error) {
                    PhotoError.READ -> com.cerebus.tokens.core.ui.R.string.storage_read_error
                    PhotoError.WRITE -> com.cerebus.tokens.core.ui.R.string.storage_write_error
                    PhotoError.SOURCE -> R.string.photo_source_unavailable
                    null -> com.cerebus.tokens.core.ui.R.string.storage_loading
                })
                if (!initialized || renderedUri != state.photoUri) {
                    initialized = true
                    renderedUri = state.photoUri
                    binding.reinforcementImage.setPhotoImage(state.photoUri?.toUri(),
                        com.cerebus.tokens.core.ui.R.drawable.baseline_add_a_photo_24)
                }
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

    private fun launchSource(action: () -> Unit) {
        try { action() }
        catch (_: ActivityNotFoundException) { viewModel.sourceUnavailable() }
        catch (_: SecurityException) { viewModel.sourceUnavailable() }
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.cancel()
        super.onCancel(dialog)
    }
}
