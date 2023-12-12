package com.cerebus.tokens.reinforcement_photo.data

import com.cerebus.tokens.data.reinforcement.storage.ReinforcementStorage
import com.cerebus.tokens.reinforcement_photo.domain.repository.ReinforcementGalleryRepository

class ReinforcementGalleryRepositoryImpl(private val reinforcementStorage: ReinforcementStorage): ReinforcementGalleryRepository {
    override fun saveSelectedPhotoUri(uri: String) {
        reinforcementStorage.savePhotoUri(uri)
    }

    override fun getSelectedPhotoUri() = reinforcementStorage.getPhotoUri()
}