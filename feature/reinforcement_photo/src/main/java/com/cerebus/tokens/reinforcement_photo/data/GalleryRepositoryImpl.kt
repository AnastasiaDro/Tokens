package com.cerebus.tokens.reinforcement_photo.data

import com.cerebus.tokens.reinforcement_photo.data.storage.GalleryStorage
import com.cerebus.tokens.reinforcement_photo.domain.repository.GalleryRepository

class GalleryRepositoryImpl(private val galleryStorage: GalleryStorage): GalleryRepository {
    override fun saveSelectedPhotoUri(uri: String) {
        galleryStorage.savePhotoUri(uri)
    }

    override fun getSelectedPhotoUri() = galleryStorage.getPhotoUri()
}