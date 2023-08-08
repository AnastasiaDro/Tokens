package com.cerebus.tokens.effects_manager

import android.content.SharedPreferences
import com.cerebus.tokens.utils.PrefsConstants
import com.cerebus.tokens.tokens_screen.TokensViewModel
import com.cerebus.tokens.settings_screen.SettingsViewModel

/**
 * [EffectsManagerImpl] - a realisation of the [EffectsManager] interface
 * Intended for containing information about win effects and modifying it
 *  - Is win animation enabled
 *  - Is win sound enabled
 *
 *  It also loads and saves information about effects to the [SharedPreferences]
 *
 * @see EffectsManager
 * @see TokensViewModel
 * @see SettingsViewModel
 *
 * @author Anastasia Drogunova
 * @since 11.07.2023
 */
class EffectsManagerImpl(private val prefs: SharedPreferences): EffectsManager {

    private var isAnimateWin = false
    private var isSoundWin = false
    private var isReady = false

    override fun setIsAnimateWin(isAnimate: Boolean) {
        isAnimateWin = isAnimate
        prefs.edit()?.putBoolean(PrefsConstants.ANIMATION_SHOWING, isAnimate)?.apply()
    }

    override fun getIsAnimateWin() = isAnimateWin

    override fun setIsSoundWin(isSound: Boolean) {
        isSoundWin = isSound
        prefs.edit()?.putBoolean(PrefsConstants.SOUND_PLAYING, isSound)?.apply()
    }

    override fun getIsSoundWin() = isSoundWin

    override suspend fun initEffectsManager() {
        prefs.getBoolean(PrefsConstants.ANIMATION_SHOWING, false).let { isAnimateWin = it }
        prefs.getBoolean(PrefsConstants.SOUND_PLAYING, false).let { isSoundWin = it }
        isReady = true
    }

    override fun isReady() = isReady

    companion object {
        private var instance: EffectsManagerImpl? = null

        fun create(prefs: SharedPreferences): EffectsManagerImpl? {
            instance = EffectsManagerImpl(prefs)
            return instance
        }

        fun getInstance() = instance
    }
}