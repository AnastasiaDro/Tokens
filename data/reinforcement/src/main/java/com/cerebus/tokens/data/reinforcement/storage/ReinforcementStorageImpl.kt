package com.cerebus.tokens.data.reinforcement.storage

import android.content.Context
import android.content.pm.PackageManager
import com.cerebus.tokens.logger.api.LoggerFactory

class ReinforcementStorageImpl(context: Context, loggerFactory: LoggerFactory) :
    ReinforcementStorage {

    private val logger = loggerFactory.createLogger(this::class.java.simpleName)

    private val prefs = context.getSharedPreferences(REINFORCEMENT_SETTINGS_PREFERENCES, Context.MODE_PRIVATE)

    private val isCameraAvailable = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    override fun isReinforcementShow() = prefs.getBoolean(REINFORCEMENT_IMAGE_SHOWING, false) && isCameraAvailable

    override fun setReinforcementShow(isShow: Boolean) {
        prefs.edit().putBoolean(REINFORCEMENT_IMAGE_SHOWING, isShow).apply()
    }

    override fun getPhotoUri(): String? {
        return prefs.getString(PHOTO_URI_STRING, null)
    }

    override fun savePhotoUri(uriString: String) {
        prefs.edit().putString(PHOTO_URI_STRING, uriString).apply()
    }

    private companion object {
        const val PHOTO_URI_STRING = "PhotoUri"

        const val REINFORCEMENT_IMAGE_SHOWING = "ReinforcementShowing"

        const val REINFORCEMENT_SETTINGS_PREFERENCES = "ReinforcementPreferences"
    }
}