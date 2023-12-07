package data.effects.storage

import android.content.Context

/**
 * [EffectsStorageImpl] - a realisation interface for storage of effects data:
 * a data about animation and sound which used here
 * is it on or off for example
 * Uses SharedPreferences as DataHolder
 *
 * @see EffectsStorage
 *
 * @since 18.11.2023
 * @author Anastasia Drogunova
 */
class EffectsStorageImpl(context: Context): EffectsStorage {

   private val prefs = context.getSharedPreferences(WIN_EFFECTS_PREFERENCES, Context.MODE_PRIVATE)
    override fun plugWinAnimationOn() {
        prefs.edit().putBoolean(ANIMATION_SHOWING, true).apply()
    }

    override fun plugWinAnimationOff() {
        prefs.edit().putBoolean(ANIMATION_SHOWING, false).apply()
    }

    override fun plugWinSoundOn() {
        prefs.edit().putBoolean(SOUND_PLAYING, true).apply()
    }

    override fun plugWinSoundOff() {
        prefs.edit().putBoolean(SOUND_PLAYING, false).apply()
    }

    override fun getIsWinSound() = prefs.getBoolean(SOUND_PLAYING, true)


    override fun getIsWinAnimation() = prefs.getBoolean(ANIMATION_SHOWING, true)


    override fun getEffectsDuration() = DEFAULT_EFFECTS_DURATION

    override fun getAnimationRepeatTimes()= ANIMATION_REPEAT_TIMES
    private companion object {

        const val ANIMATION_SHOWING = "AnimationShowing"
        const val SOUND_PLAYING = "SoundPlaying"

        const val DEFAULT_EFFECTS_DURATION = 5000L
        const val ANIMATION_REPEAT_TIMES = 1

        const val WIN_EFFECTS_PREFERENCES = "WinEffectsPreferences"
    }
}