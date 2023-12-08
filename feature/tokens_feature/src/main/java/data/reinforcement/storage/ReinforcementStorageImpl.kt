package data.reinforcement.storage

import android.content.Context
import com.cerebus.tokens.logger.api.LoggerFactory

class ReinforcementStorageImpl(context: Context, loggerFactory: LoggerFactory) : ReinforcementStorage {

    private val logger = loggerFactory.createLogger(this::class.java.simpleName)

    private val prefs = context.getSharedPreferences(REINFORCEMENT_SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
    override fun isReinforcementShow() = prefs.getBoolean(REINFORCEMENT_IMAGE_SHOWING, true)

    override fun setReinforcementShow(isShow: Boolean) {
        prefs.edit().putBoolean(REINFORCEMENT_IMAGE_SHOWING, isShow).apply()
    }

    private companion object {
        const val REINFORCEMENT_IMAGE_SHOWING = "ReinforcementShowing"

        const val REINFORCEMENT_SETTINGS_PREFERENCES = "ReinforcementSettingsPreferences"
    }
}