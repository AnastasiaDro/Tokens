package com.cerebus.tokens.reinforcement_photo.domain.usecases

import com.cerebus.tokens.reinforcement_photo.domain.repository.GalleryRepository

class GetSelectedPhotoUriUseCase(private val repository: GalleryRepository) {

    fun execute() = repository.getSelectedPhotoUri()
}