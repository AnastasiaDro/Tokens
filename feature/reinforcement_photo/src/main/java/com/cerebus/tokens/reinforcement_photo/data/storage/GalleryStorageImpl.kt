package com.cerebus.tokens.reinforcement_photo.data.storage

import android.content.Context
import com.cerebus.tokens.logger.api.LoggerFactory

class GalleryStorageImpl(context: Context, loggerFactory: LoggerFactory) : GalleryStorage  {

    private val logger = loggerFactory.createLogger(this::class.java.simpleName)

    private val prefs = context.getSharedPreferences(PHOTO_PREFERENCES, Context.MODE_PRIVATE)
    override fun getPhotoUri(): String? {
        return prefs.getString(PHOTO_URI_STRING, null)
    }

    override fun savePhotoUri(uriString: String) {
        prefs.edit().putString(PHOTO_URI_STRING, uriString).apply()
    }
    private companion object {
        const val PHOTO_URI_STRING = "PhotoUri"

        const val PHOTO_PREFERENCES = "ReinforcementSettingsPreferences"
    }
}
