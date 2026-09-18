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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.core.ui.PermissionsManager.isPermissionGranted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
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
    private val repository: ReinforcementRepository
) : ViewModel() {

    private var currentPhotoUri: Uri? = null
    fun getUri() = currentPhotoUri

    private val mutableOpenSourceSharedFlow = MutableSharedFlow<ImageSource>()
    val openSourceSharedFlow: SharedFlow<ImageSource> = mutableOpenSourceSharedFlow

    private val permissionMutableSharedFlow = MutableSharedFlow<PermissionType>()
    val permissionSharedFlow: SharedFlow<PermissionType> = permissionMutableSharedFlow

    private val mutableState = MutableStateFlow(PhotoUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var retrySave: (() -> Unit)? = null

    private val showMessageSharedFlow = MutableSharedFlow<String>()
    val messageSharedFlow: SharedFlow<String> = showMessageSharedFlow

    init { observe() }

    private fun observe(retryPendingSave: Boolean = false) {
        observation?.cancel()
        mutableState.update { it.copy(loading = true) }
        observation = viewModelScope.launch {
            var shouldRetrySave = retryPendingSave
            try {
                repository.settings.collect { value ->
                    mutableState.update { it.copy(photoUri = value.photoUri, loading = false,
                        readFailure = false) }
                    if (shouldRetrySave) {
                        shouldRetrySave = false
                        retryFailedSave()
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, readFailure = true) } }
        }
    }

    fun retry() {
        val current = state.value
        when {
            current.readFailure -> observe(current.writeFailure && retrySave != null)
            current.writeFailure -> retryFailedSave()
            else -> observe()
        }
    }

    private fun retryFailedSave() {
        val action = retrySave ?: return
        retrySave = null
        action()
    }

    internal fun saveSelection(uri: String?, acquireAccess: suspend () -> Unit = {}) {
        if (uri == null || state.value.loading || state.value.saving || state.value.saved || state.value.error == PhotoError.READ) return
        retrySave = null
        mutableState.update { it.copy(saving = true, writeFailure = false) }
        viewModelScope.launch {
            try {
                acquireAccess()
                repository.setPhotoUri(uri)
                retrySave = null
                mutableState.update { it.copy(saving = false, saved = true, writeFailure = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                retrySave = { saveSelection(uri, acquireAccess) }
                mutableState.update { it.copy(saving = false, writeFailure = true) }
            }
        }
    }

    fun makePhoto(context: Context) {
        askToMakePhoto(context)
    }

    private fun askToMakePhoto(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !isPermissionGranted(context, WRITE_EXTERNAL_STORAGE)) {
            viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionType.WRITE_STORAGE_PERMISSION) }
        } else {
            if (isPermissionGranted(context, CAMERA)) {
                getPhotoFile(context)
                openCamera()
            }
            else
                viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionType.CAMERA_PERMISSION) }
        }
    }

    fun getFromGallery(context: Context) {
        askToGetFromGallery(context)
    }

    private fun askToGetFromGallery(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && isPermissionGranted(context, android.Manifest.permission.READ_EXTERNAL_STORAGE))
            askSdkUnderTiramisu(context)
        else
            askSdkTiramisuAndAbove(context)
    }

    private fun askSdkTiramisuAndAbove(context: Context) {
        if (isPermissionGranted(context, android.Manifest.permission.READ_MEDIA_IMAGES))
            openGallery()
        else
            viewModelScope.launch { permissionMutableSharedFlow.emit(PermissionType.READ_MEDIA_IMAGES) }
    }

    private fun askSdkUnderTiramisu(context: Context) {
        if (isPermissionGranted(context, android.Manifest.permission.READ_EXTERNAL_STORAGE))
            openGallery()
        else
            viewModelScope.launch {
                permissionMutableSharedFlow.emit(PermissionType.READ_STORAGE_PERMISSION)
            }
    }

    fun onPermissionResultReceive(permissionType: PermissionType, isGranted: Boolean, context: Context) {
        if (isGranted) {
            when (permissionType) {
                PermissionType.WRITE_STORAGE_PERMISSION -> askToMakePhoto(context)
                PermissionType.CAMERA_PERMISSION -> askToMakePhoto(context)
                PermissionType.READ_STORAGE_PERMISSION -> askToGetFromGallery(context)
                PermissionType.READ_MEDIA_IMAGES -> askToGetFromGallery(context)
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

    private fun removeJunkFileFromGallery(context: Context) {
        currentPhotoUri?.let { context.contentResolver.delete(it, null, null) }
    }

    fun onCameraResultReceived(context: Context, success: Boolean) {
        if (success) {
            val appContext = context.applicationContext
            saveSelection(currentPhotoUri?.toString()) { galleryAddPic(appContext) }
        } else {
            try { removeJunkFileFromGallery(context) }
            catch (_: Exception) { showMessage("No photo made") }
        }
    }

    fun onGalleryResultReceived(context: Context, selectedImageUri: Uri?) {
        val appContext = context.applicationContext
        saveSelection(selectedImageUri?.toString()) {
            val uri = requireNotNull(selectedImageUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appContext.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                appContext.grantUriPermission(appContext.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
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

    private fun showMessage(message: String) {
        viewModelScope.launch { showMessageSharedFlow.emit(message) }
    }
}
