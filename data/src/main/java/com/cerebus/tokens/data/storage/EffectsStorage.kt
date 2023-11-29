package com.cerebus.tokens.data.storage

interface EffectsStorage {

    fun plugWinAnimationOn()

    fun plugWinAnimationOff()

    fun plugWinSoundOn()

    fun plugWinSoundOff()

    fun getIsWinAnimation(): Boolean

    fun getIsWinSound(): Boolean

    fun getEffectsDuration(): Long

    fun getAnimationRepeatTimes(): Int
}