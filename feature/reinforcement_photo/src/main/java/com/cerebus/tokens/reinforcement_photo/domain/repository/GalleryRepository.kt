package com.cerebus.tokens.reinforcement_photo.domain.repository

interface GalleryRepository {

    fun saveSelectedPhotoUri(uri: String)

    fun getSelectedPhotoUri(): String?


}