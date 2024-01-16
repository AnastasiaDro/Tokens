package com.cerebus.tokens.reinforcement_photo.presentation

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.core.ui.PermissionsManager.isPermissionGranted
import com.cerebus.tokens.reinforcement_photo.domain.usecases.GetSelectedPhotoPathUseCase
import com.cerebus.tokens.reinforcement_photo.domain.usecases.SaveSelectedPhotoPathUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [ChangePhotoViewModel] - a viewModel class for [AskForReinforcementImageDialog]
 *
 * Parses the cases of opening a camera or a gallery for
 * new reinforcement image selecting
 *
 * @see AskForReinforcementImageDialog
 *
 * @author Anastasia Drogunova
 * @since 25.12.2023
 */
class ChangePhotoViewModel(
    private val getSelectedPhotoPathUseCase: GetSelectedPhotoPathUseCase,
    private val saveSelectedPhotoPathUseCase: SaveSelectedPhotoPathUseCase
) : ViewModel() {

    private var currentPhotoUri: Uri? = null
    fun getUri() = currentPhotoUri

    private val mutableOpenSourceSharedFlow = MutableSharedFlow<ImageSource>()
    val openSourceSharedFlow: SharedFlow<ImageSource> = mutableOpenSourceSharedFlow

    private val permissionMutableSharedFlow = MutableSharedFlow<PermissionType>()
    val permissionSharedFlow: SharedFlow<PermissionType> = permissionMutableSharedFlow

    private val placePhotoStateFlow =
        MutableStateFlow(getSelectedPhotoPathUseCase.execute()?.toUri())
    val photoUriStateFlow = placePhotoStateFlow

    private val showMessageSharedFlow = MutableSharedFlow<String>()
    val messageSharedFlow: SharedFlow<String> = showMessageSharedFlow

    private val setNavResultSharedFlow = MutableSharedFlow<Boolean>()
    val navResultSharedFlow: SharedFlow<Boolean> = setNavResultSharedFlow

    fun makePhoto(context: Context) {
        askToMakePhoto(context)
    }

    private fun askToMakePhoto(context: Context) {
//        if (!isPermissionGranted(context, WRITE_EXTERNAL_STORAGE)) {
//            println("НАстя !isPermissionGranted(context, WRITE_EXTERNAL_STORAGE)")
//            viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionType.WRITE_STORAGE_PERMISSION) }
//        } else {
            if (isPermissionGranted(context, CAMERA)) {
                getPhotoFile(context)
                openCamera()
            }
            else
                viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionType.CAMERA_PERMISSION) }
        //}
    }

    fun getFromGallery(context: Context) {
        askToGetFromGallery(context)
    }

    private fun askToGetFromGallery(context: Context) {
       // if (isPermissionGranted(context, android.Manifest.permission.READ_EXTERNAL_STORAGE))
        if (isPermissionGranted(context, android.Manifest.permission.READ_MEDIA_IMAGES))
            openGallery()
        else
            //viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionType.READ_STORAGE_PERMISSION) }
            viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionType.READ_STORAGE_PERMISSION) }
    }

    fun onPermissionResultReceive(permissionType: PermissionType, isGranted: Boolean, context: Context) {
        println("Настя onPermissionResultReceive isGranted = $isGranted")
        if (isGranted) {
            when (permissionType) {
                PermissionType.WRITE_STORAGE_PERMISSION -> askToMakePhoto(context)
                PermissionType.CAMERA_PERMISSION -> askToMakePhoto(context)
                PermissionType.READ_STORAGE_PERMISSION -> askToGetFromGallery(context)
            }
        } else {
            showMessage(permissionType.errorMessage)
        }
    }

    private fun openCamera() = viewModelScope.launch {
        mutableOpenSourceSharedFlow.emit(ImageSource.CAMERA)
    }

    private fun openGallery() = viewModelScope.launch {
        mutableOpenSourceSharedFlow.emit(ImageSource.GALLERY)
    }

    private fun savePhotoUri(context: Context, uri: Uri?) {
        uri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } else {
                context.grantUriPermission(
                    context.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            saveUriToStorage(uri)
        }
    }

    private fun saveUriCamera() {
        currentPhotoUri?.let { saveUriToStorage(it) }
    }


    private fun removeJunkFileFromGallery(context: Context) {
        currentPhotoUri?.let { context.contentResolver.delete(it, null, null) }
    }

    fun onCameraResultReceived(context: Context, success: Boolean) {
        try {
            if (success) {
                saveUriCamera()
                galleryAddPic(context)
            } else {
                removeJunkFileFromGallery(context)
            }
            viewModelScope.launch {
                setNavResultSharedFlow.emit(success)
            }
        } catch (e: Exception) {
            showMessage("No photo made")
        }
    }

    fun onGalleryResultReceived(context: Context, selectedImageUri: Uri?) {
        try {
            savePhotoUri(context, selectedImageUri)
            viewModelScope.launch { setNavResultSharedFlow.emit(true) }
        } catch (e: Exception) {
            showMessage("No image selected")
            viewModelScope.launch { setNavResultSharedFlow.emit(false) }
        }
    }

   private fun galleryAddPic(context: Context) {

        currentPhotoUri?.let { mediaScanUri ->
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(mediaScanUri.path),
                null
            ) { _, uri ->
                Log.i("Scanner", "Scanned $uri")
            }
        }
    }

    private fun getPhotoFile(context: Context) {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val uniqueFileName = "JPEG_${timeStamp}_"

        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, uniqueFileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        currentPhotoUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
    }

    /**
     * @param imageUri - an uri of the new reinforcement image
     * which could be taken from the camera or gallery
     **/
    private fun saveUriToStorage(imageUri: Uri) {
        saveSelectedPhotoPathUseCase.execute(imageUri.toString())
        viewModelScope.launch {
            placePhotoStateFlow.emit(
                getSelectedPhotoPathUseCase.execute()?.toUri()
            )
        }
    }

    private fun showMessage(message: String) {
        viewModelScope.launch { showMessageSharedFlow.emit(message) }
    }
}
