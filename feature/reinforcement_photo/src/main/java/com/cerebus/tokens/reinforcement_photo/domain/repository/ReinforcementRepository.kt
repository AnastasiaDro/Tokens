package com.cerebus.tokens.reinforcement_photo.domain.repository

interface ReinforcementRepository {

    fun makePhoto()

    fun getPhoto()

    fun takePhotoFromGallery()
}