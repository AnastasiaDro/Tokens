package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.core.ui.getRealPathFromURI
import com.cerebus.tokens.reinforcement_photo.domain.usecases.GetSelectedPhotoPathUseCase
import com.cerebus.tokens.reinforcement_photo.domain.usecases.SaveSelectedPhotoPathUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ChangePhotoViewModel(
    private val getSelectedPhotoPathUseCase: GetSelectedPhotoPathUseCase,
    private val saveSelectedPhotoPathUseCase: SaveSelectedPhotoPathUseCase
): ViewModel() {

    private val mutableChangePhotoSharedFlow = MutableSharedFlow<ChangingPhoto>()
    val changePhotoSharedFlow: SharedFlow<ChangingPhoto> = mutableChangePhotoSharedFlow

    private val placePhotoStateFlow = MutableStateFlow<String?>(getSelectedPhotoPathUseCase.execute())
    val photoUriStateFlow = placePhotoStateFlow
    fun askPhotoFromCamera() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_CAMERA)
    }

    fun askPhotoFromGallery() = viewModelScope.launch {
        mutableChangePhotoSharedFlow.emit(ChangingPhoto.GET_FROM_GALLERY)
    }

    fun savePhotoUri(context: Context, uri: Uri?) {
        uri?.let {
            getRealPathFromURI(context, uri)?.let {
                saveSelectedPhotoPathUseCase.execute(it)
            }
            viewModelScope.launch { placePhotoStateFlow.emit(getSelectedPhotoPathUseCase.execute()) }
        }
    }
}