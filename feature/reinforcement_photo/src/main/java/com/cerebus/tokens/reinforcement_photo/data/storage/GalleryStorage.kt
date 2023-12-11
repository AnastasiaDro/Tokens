package com.cerebus.tokens.reinforcement_photo.data.storage

interface GalleryStorage {

    fun getPhotoUri(): String?

    fun savePhotoUri(uriString: String)
}