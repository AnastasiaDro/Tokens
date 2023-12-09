package com.cerebus.tokens.reinforcement_photo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ChangePhotoViewModel: ViewModel() {

    private val mutableChangePhotoSharedFlow = MutableSharedFlow<ChangingPhoto>()
    val changePhotoSharedFlow: SharedFlow<ChangingPhoto> = mutableChangePhotoSharedFlow
    fun getPhotoFromCamera() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_CAMERA)
    }

    fun getPhotoFromGallery() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_GALLERY)
    }


}