package com.cerebus.tokens.reinforcement_photo.domain.usecases

import com.cerebus.tokens.reinforcement_photo.domain.repository.ReinforcementGalleryRepository

class SaveSelectedPhotoPathUseCase(private val repository: ReinforcementGalleryRepository) {

    fun execute(uriString: String) {
        repository.saveSelectedPhotoUri(uriString)
    }
}