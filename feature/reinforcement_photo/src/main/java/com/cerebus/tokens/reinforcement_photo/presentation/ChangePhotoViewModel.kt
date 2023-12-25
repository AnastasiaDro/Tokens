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


//Логика:


class ChangePhotoViewModel(
    private val getSelectedPhotoPathUseCase: GetSelectedPhotoPathUseCase,
    private val saveSelectedPhotoPathUseCase: SaveSelectedPhotoPathUseCase
) : ViewModel() {

    private var currentPhotoUri: Uri? = null
    fun getUri() = currentPhotoUri

    private val mutableChangePhotoSharedFlow = MutableSharedFlow<ChangingPhoto>()
    val changePhotoSharedFlow: SharedFlow<ChangingPhoto> = mutableChangePhotoSharedFlow


    private val permissionMutableSharedFlow = MutableSharedFlow<PermissionsEnum>()
    val permissionSharedFlow: SharedFlow<PermissionsEnum> = permissionMutableSharedFlow

    fun askToMakePhoto(context: Context) {
        if (!isPermissionGranted(context, WRITE_EXTERNAL_STORAGE)) {
            viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionsEnum.WRITE_STORAGE_PERMISSION) }
        } else {
            if (isPermissionGranted(context, CAMERA))
                getPhotoFromCamera()
            else
                viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionsEnum.CAMERA_PERMISSION) }
        }
    }

    fun askToGetFromGallery(context: Context) {
        if (isPermissionGranted(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            println("Настя askToGetFromGallery GRANTED")
            getPhotoFromGallery()
        }

        else {
            println("Настя askToGetFromGallery NOT GRANTED")
            viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionsEnum.READ_STORAGE_PERMISSION) }
        }

    }


    private val placePhotoStateFlow =
        MutableStateFlow<Uri?>(getSelectedPhotoPathUseCase.execute()?.toUri())
    val photoUriStateFlow = placePhotoStateFlow
    private fun getPhotoFromCamera() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_CAMERA)
    }

    private fun getPhotoFromGallery() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_GALLERY)
    }

    fun savePhotoUri(context: Context, uri: Uri?) {
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

    fun saveUriCamera() {
        currentPhotoUri?.let { saveUriToStorage(it) }
    }

    fun removeFromGallery(context: Context) {
        currentPhotoUri?.let { context.contentResolver.delete(it, null, null) }
    }

    fun galleryAddPic(context: Context) {

        println("Настя 2 SDK >= 10")
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

    fun getPhotoFile(context: Context) {
        // Создаем файл для сохранения изображения

        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val uniqueFileName = "JPEG_${timeStamp}_"

        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, uniqueFileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        currentPhotoUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!
    }

    private fun saveUriToStorage(uri: Uri) {
        saveSelectedPhotoPathUseCase.execute(uri.toString())
        viewModelScope.launch {
            placePhotoStateFlow.emit(
                getSelectedPhotoPathUseCase.execute()?.toUri()
            )
        }
    }
}