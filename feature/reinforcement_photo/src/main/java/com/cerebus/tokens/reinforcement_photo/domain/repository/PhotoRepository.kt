package com.cerebus.tokens.reinforcement_photo.domain.repository

interface PhotoRepository {

    fun makePhoto()

    fun getPhotoFromStorage()
}