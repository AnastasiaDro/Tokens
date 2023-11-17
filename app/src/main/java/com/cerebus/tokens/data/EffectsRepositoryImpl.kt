package com.cerebus.tokens.data

import android.content.SharedPreferences
import com.cerebus.tokens.domain.models.WinEffects
import com.cerebus.tokens.domain.repository.EffectsRepository

class EffectsRepositoryImpl(private val prefs: SharedPreferences) : EffectsRepository {

    private val winEffects = WinEffects(
        isWinAnimationOn = prefs.getBoolean(ANIMATION_SHOWING, true),
        isWinSoundOn = prefs.getBoolean(SOUND_PLAYING, true),
        duration = DEFAULT_EFFECTS_DURATION,
        animationRepeatTimes = ANIMATION_REPEAT_TIMES
    )
    override fun plugWinAnimationOn() {
        winEffects.isWinAnimationOn = true
        prefs.edit().putBoolean(ANIMATION_SHOWING, true).apply()
    }

    override fun plugWinAnimationOff() {
        winEffects.isWinAnimationOn = false
        prefs.edit().putBoolean(ANIMATION_SHOWING, false).apply()
    }

    override fun plugWinSoundOn() {
        winEffects.isWinSoundOn = true
        prefs.edit().putBoolean(SOUND_PLAYING, true).apply()
    }

    override fun plugWinSoundOff() {
        winEffects.isWinSoundOn = false
        prefs.edit().putBoolean(SOUND_PLAYING, false).apply()
    }

    override fun getWinEffects() = winEffects

    private companion object {

        const val ANIMATION_SHOWING = "AnimationShowing"
        const val SOUND_PLAYING = "SoundPlaying"


        const val DEFAULT_EFFECTS_DURATION = 5000L
        const val ANIMATION_REPEAT_TIMES = 1
    }
}