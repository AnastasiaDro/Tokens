package com.cerebus.tokens.reinforcement_photo.domain.repository

interface ReinforcementGalleryRepository {

    fun saveSelectedPhotoUri(uri: String)

    fun getSelectedPhotoUri(): String?


}