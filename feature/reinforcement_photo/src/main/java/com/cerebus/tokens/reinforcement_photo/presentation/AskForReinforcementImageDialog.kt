package com.cerebus.tokens.reinforcement_photo.presentation

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.setNavigationResult
import com.cerebus.tokens.core.ui.showToast
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.logger.api.LoggerFactory
import com.cerebus.tokens.reinforcement_photo.R
import com.cerebus.tokens.reinforcement_photo.databinding.DialogAskForReinforcementImageBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * [AskForReinforcementImageDialog] - a dialog for selecting a new image of the reinforcement
 *
 * @author Anastasia Drogunova
 * @since 09.12.2023
 */
class AskForReinforcementImageDialog : DialogFragment(R.layout.dialog_ask_for_reinforcement_image) {

    private val viewBinding: DialogAskForReinforcementImageBinding by viewBinding()
    private val loggerFactory: LoggerFactory by inject()
    private val logger = loggerFactory.createLogger(this::class.java.simpleName)
    private val viewModel: ChangePhotoViewModel by viewModel()

    private val getFromGalleryResultLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { selectedImageUri ->
        viewModel.onGalleryResultReceived(requireContext(), selectedImageUri)
    }

    private val getFromCameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraResultReceived(requireContext(), success)
    }

    private val requestWriteStoragePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            viewModel.onPermission(PermissionsEnum.WRITE_STORAGE_PERMISSION, isGranted, requireContext())
        }

    private val requestGalleryPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            viewModel.onPermission(PermissionsEnum.READ_STORAGE_PERMISSION, isGranted, requireContext())
        }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.onPermission(PermissionsEnum.CAMERA_PERMISSION, isGranted, requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
        subscribeToViewModel()
    }

    private fun initButtons() = with(viewBinding) {
        cancelButton.setOnClickListener {
            dismiss()
            logger.d("cancel photo selection")
        }
        makePhotoButton.setOnClickListener {
            viewModel.makePhoto(requireContext())
            logger.d("ask to make a photo")
        }
        getFromGalleryButton.setOnClickListener {
            viewModel.getFromGallery(requireContext())
            logger.d("ask to get an image from gallery")
        }
    }

    private fun openGallery() {
        getFromGalleryResultLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun openCamera() {
        getFromCameraResultLauncher.launch(viewModel.getUri())
    }

    private fun subscribeToViewModel() {

        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.openSourceSharedFlow) { source ->
            when (source) {
                ImageSource.CAMERA -> openCamera()
                ImageSource.GALLERY -> openGallery()
            }
        }

        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.photoUriStateFlow) { imageUri ->
            imageUri?.let {
                viewBinding.reinforcementImage.setImageURI(null)
                viewBinding.reinforcementImage.setImageURI(imageUri)
            }
        }

        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.permissionSharedFlow) { permissionType ->
            when(permissionType) {
                PermissionsEnum.WRITE_STORAGE_PERMISSION ->  requestWriteStoragePermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
                PermissionsEnum.CAMERA_PERMISSION -> requestCameraPermissionLauncher.launch(CAMERA)
                PermissionsEnum.READ_STORAGE_PERMISSION -> requestGalleryPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
            }
        }

        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.messageSharedFlow) { message ->
            showToast(message)
        }

        subscribeToHotFlow(Lifecycle.State.CREATED, viewModel.navResultSharedFlow) { navResult ->
            setNavigationResult(navResult, IS_IMAGE_SET_RESULT)
            if (navResult) dismiss()
        }
    }

    companion object {
        const val IS_IMAGE_SET_RESULT = "ImageUri"
    }
}