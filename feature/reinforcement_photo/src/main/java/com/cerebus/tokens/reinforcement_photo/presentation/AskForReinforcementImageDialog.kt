package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.setNavigationResult
import com.cerebus.tokens.logger.api.LoggerFactory
import com.cerebus.tokens.reinforcement_photo.R
import com.cerebus.tokens.reinforcement_photo.databinding.DialogAskForReinforcementImageBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private var currentPhotoUri: Uri? = null

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

    private val requestGalleryPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.askPhotoFromGallery()
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Can't open a gallery without permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.askPhotoFromCamera()
        } else {
            Toast.makeText(
                requireActivity(),
                "Can't open a camera without permission",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val getFromCameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        try {
            if (success) {
                viewModel.saveUriCamera(currentPhotoUri)
                galleryAddPic()
                setNavigationResult(true, IS_IMAGE_SET_RESULT)
                dismiss()
            } else {
                currentPhotoUri?.let { removeFromGallery(it) }
            }
        } catch (e: Exception) {
            setNavigationResult(false, IS_IMAGE_SET_RESULT)
            Toast.makeText(requireActivity(), "No image selected", Toast.LENGTH_LONG).show()
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

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
                viewModel.askPhotoFromCamera()
            else
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            logger.d("ask to make a photo")
        }

        getFromGalleryButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            )
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
        getPhotoFile()
        getFromCameraResultLauncher.launch(currentPhotoUri)
    }


    /** Обновление галереи, чтобы отобразить новое изображение **/
    private fun galleryAddPic() {

        println("Настя 2 SDK >= 10")
        currentPhotoUri?.let { mediaScanUri ->
            MediaScannerConnection.scanFile(
                requireContext().applicationContext,
                arrayOf(mediaScanUri.path),
                null
            ) { _, uri ->
                Log.i("Scanner", "Scanned $uri")
            }
        }
    }

    private fun getPhotoFile() {
        // Создаем файл для сохранения изображения

        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val uniqueFileName = "JPEG_${timeStamp}_"

        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, uniqueFileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        currentPhotoUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!
    }

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
    }

    private fun removeFromGallery(uri: Uri) {
        requireContext().contentResolver.delete(uri, null, null)
    }

    companion object {
        const val IS_IMAGE_SET_RESULT = "ImageUri"
    }
}