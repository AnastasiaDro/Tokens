package com.cerebus.tokens.reinforcement_photo.presentation

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.PermissionsManager.isPermissionGranted
import com.cerebus.tokens.core.ui.setNavigationResult
import com.cerebus.tokens.core.ui.showToast
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
    ) { result ->
        try {
            viewModel.savePhotoUri(requireContext(), result)
            setNavigationResult(true, IS_IMAGE_SET_RESULT)
            dismiss()
        } catch (e: Exception) {
            setNavigationResult(false, IS_IMAGE_SET_RESULT)
            Toast.makeText(requireActivity(), "No image selected", Toast.LENGTH_LONG).show()
        }
    }

    private val requestWriteStoragePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            isGranted: Boolean ->
            if (!isGranted) {
                showToast("Can't open camera without permission")
                dismiss()
            } else {
                viewModel.askToMakePhoto(requireContext())
//                if (isPermissionGranted(requireContext(), android.Manifest.permission.CAMERA))
//                    viewModel.askPhotoFromCamera()
//                else
//                    requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                logger.d("ask to make a photo")
            }
        }

    private val requestGalleryPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.askPhotoFromGallery()
            } else {
                showToast("Can't open a gallery without permission")
            }
        }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.askToMakePhoto(requireContext())
        } else {
            showToast("Can't open a camera without permission")
        }
    }

    private val getFromCameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        try {
            if (success) {
                viewModel.saveUriCamera()
                viewModel.galleryAddPic(requireContext())
                setNavigationResult(true, IS_IMAGE_SET_RESULT)
                dismiss()
            } else {
                viewModel.removeFromGallery(requireContext())
            }
        } catch (e: Exception) {
            setNavigationResult(false, IS_IMAGE_SET_RESULT)
            showToast("No image selected")
            e.stackTrace
        }
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
            viewModel.askToMakePhoto(requireContext())
        }

        getFromGalleryButton.setOnClickListener {
            if (isPermissionGranted(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE))
                viewModel.askPhotoFromGallery()
            else
                requestGalleryPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            logger.d("ask to get a photo from gallery")
        }
    }

    private fun openGallery() {
        getFromGalleryResultLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun openCamera() {
        viewModel.getPhotoFile(requireContext())
        getFromCameraResultLauncher.launch(viewModel.getUri())
    }


    /** Обновление галереи, чтобы отобразить новое изображение **/



    private fun subscribeToViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.changePhotoSharedFlow.collect {
                    when (it) {
                        ChangingPhoto.GET_FROM_CAMERA -> openCamera()
                        ChangingPhoto.GET_FROM_GALLERY -> openGallery()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.photoUriStateFlow.collect {
                    it?.let {
                        viewBinding.reinforcementImage.setImageURI(null)
                        viewBinding.reinforcementImage.setImageURI(it)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.permissionSharedFlow.collect { permissionType ->
                    when(permissionType) {
                        PermissionsEnum.WRITE_STORAGE_PERMISSION ->  requestWriteStoragePermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
                        PermissionsEnum.CAMERA_PERMISSION -> requestCameraPermissionLauncher.launch(CAMERA)
                        else -> { }//TODO
                    }
                }
            }
        }
    }

    companion object {
        const val IS_IMAGE_SET_RESULT = "ImageUri"
    }
}