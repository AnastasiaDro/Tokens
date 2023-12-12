package com.cerebus.tokens.reinforcement_photo.domain.usecases

import com.cerebus.tokens.reinforcement_photo.domain.repository.ReinforcementGalleryRepository

class GetSelectedPhotoPathUseCase(private val repository: ReinforcementGalleryRepository) {

    fun execute() = repository.getSelectedPhotoUri()
}