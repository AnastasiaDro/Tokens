package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.getImageByFilePath
import com.cerebus.tokens.logger.api.LoggerFactory
import com.cerebus.tokens.reinforcement_photo.R
import com.cerebus.tokens.reinforcement_photo.databinding.DialogAskForReinforcementImageBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * [AskForReinforcementImageDialog] - a dialog for selecting a new image of the reinforcement
 *
 * @author Anastasia Drogunova
 * @since 09.12.2023
 */
class AskForReinforcementImageDialog: DialogFragment(R.layout.dialog_ask_for_reinforcement_image) {

    private val viewBinding: DialogAskForReinforcementImageBinding by viewBinding()
    private val loggerFactory: LoggerFactory by inject()
    private val logger = loggerFactory.createLogger(this::class.java.simpleName)
    private val viewModel: ChangePhotoViewModel by viewModel()

    private val getFromGalleryResultLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { result ->
        try {
            viewModel.savePhotoUri(requireContext(), result)
        } catch (e: Exception) {
            Toast.makeText(requireActivity(), "No image selected", Toast.LENGTH_LONG).show()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d("dialog view created")
        initButtons()
        subscribeToViewModel()
    }

    private fun initButtons() = with(viewBinding) {

        cancelButton.setOnClickListener {
            dismiss()
            logger.d("cancel photo selection")
        }
        makePhotoButton.setOnClickListener {
            viewModel.askPhotoFromCamera()
            logger.d("ask to make a photo")
        }
        getFromGalleryButton.setOnClickListener {
            viewModel.askPhotoFromGallery()
            logger.d("ask to get a photo from gallery")
        }
    }

    private fun openGallery() {
        getFromGalleryResultLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun subscribeToViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.changePhotoSharedFlow.collect {
                    when(it) {
                        ChangingPhoto.GET_FROM_CAMERA -> { }
                        ChangingPhoto.GET_FROM_GALLERY -> openGallery()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.photoUriStateFlow.collect {
                    it?.let {
                        viewBinding.reinforcementImage.setImageURI(it)
                    }
                }
            }
        }
    }
}