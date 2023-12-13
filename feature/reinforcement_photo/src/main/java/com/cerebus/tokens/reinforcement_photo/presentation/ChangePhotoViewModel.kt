package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.reinforcement_photo.domain.usecases.GetSelectedPhotoPathUseCase
import com.cerebus.tokens.reinforcement_photo.domain.usecases.SaveSelectedPhotoPathUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ChangePhotoViewModel(
    private val getSelectedPhotoPathUseCase: GetSelectedPhotoPathUseCase,
    private val saveSelectedPhotoPathUseCase: SaveSelectedPhotoPathUseCase
) : ViewModel() {

    private val mutableChangePhotoSharedFlow = MutableSharedFlow<ChangingPhoto>()
    val changePhotoSharedFlow: SharedFlow<ChangingPhoto> = mutableChangePhotoSharedFlow

    private val placePhotoStateFlow =
        MutableStateFlow<Uri?>(getSelectedPhotoPathUseCase.execute()?.toUri())
    val photoUriStateFlow = placePhotoStateFlow
    fun askPhotoFromCamera() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_CAMERA)
    }

    fun askPhotoFromGallery() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_GALLERY)
    }

    fun savePhotoUri(context: Context, uri: Uri?) {
        uri?.let {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
            saveUriToStorage(uri)
        }
    }

    fun saveUriCamera(uri: Uri?) {
        if (uri != null) saveUriToStorage(uri)
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