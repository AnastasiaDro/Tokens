package com.cerebus.tokens.reinforcement_photo.presentation

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.reinforcement_photo.domain.usecases.GetSelectedPhotoUriUseCase
import com.cerebus.tokens.reinforcement_photo.domain.usecases.SaveSelectedPhotoUriUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ChangePhotoViewModel(
    private val getSelectedPhotoUriUseCase: GetSelectedPhotoUriUseCase,
    private val saveSelectedPhotoUriUseCase: SaveSelectedPhotoUriUseCase
): ViewModel() {

    private val mutableChangePhotoSharedFlow = MutableSharedFlow<ChangingPhoto>()
    val changePhotoSharedFlow: SharedFlow<ChangingPhoto> = mutableChangePhotoSharedFlow

    private val placePhotoStateFlow = MutableStateFlow<Uri?>(getSelectedPhotoUriUseCase.execute()?.toUri())
    val photoUriStateFlow = placePhotoStateFlow
    fun askPhotoFromCamera() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_CAMERA)
    }

    fun askPhotoFromGallery() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_GALLERY)
    }

    fun savePhotoUri(uri: Uri?) {
        uri?.let {
            saveSelectedPhotoUriUseCase.execute(uri.toString())
            viewModelScope.launch { placePhotoStateFlow.emit(uri) }
        }
    }
}