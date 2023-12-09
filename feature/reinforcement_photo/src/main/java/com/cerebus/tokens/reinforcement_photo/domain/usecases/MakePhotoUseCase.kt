package com.cerebus.tokens.reinforcement_photo.domain.usecases

import com.cerebus.tokens.reinforcement_photo.domain.repository.PhotoRepository

class MakePhotoUseCase(private val repository: PhotoRepository) {

    fun execute() = repository.makePhoto()
}