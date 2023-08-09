package com.cerebus.tokens.utils

import com.cerebus.tokens.tokens_manager.TokensManager
import com.cerebus.tokens.tokens_manager.TokensManagerImpl

/**
 * [PrefsConstants] - an object with constants for
 * saving app settings in SharedPreferences
 *
 * @see TokensManager
 * @see TokensManagerImpl
 *
 * @author Anastasia Drogunova
 * @since 28.04.2023
 */
object PrefsConstants {
    const val TOKENS_PREFERENCES = "TokensPreferences"
    const val TOKENS_NUMBER = "TokensNumber"
    const val CHECKED_TOKENS_NUMBER = "CheckedTokensNumber"
    const val CHECKED_TOKENS_COLOR = "CheckedTokensColor"
    const val ANIMATION_SHOWING = "AnimationShowing"
    const val SOUND_PLAYING = "SoundPlaying"
}