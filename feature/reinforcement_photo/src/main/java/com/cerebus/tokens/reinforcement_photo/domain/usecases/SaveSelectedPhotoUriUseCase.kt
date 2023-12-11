package com.cerebus.tokens.reinforcement_photo.domain.usecases

import com.cerebus.tokens.reinforcement_photo.domain.repository.GalleryRepository

class SaveSelectedPhotoUriUseCase(private val repository: GalleryRepository) {

    fun execute(uriString: String) {
        repository.saveSelectedPhotoUri(uriString)
    }
}