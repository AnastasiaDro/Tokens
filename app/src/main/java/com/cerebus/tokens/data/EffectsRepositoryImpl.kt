package com.cerebus.tokens.data

import com.cerebus.tokens.data.storage.EffectsStorage
import com.cerebus.tokens.domain.models.WinEffects
import com.cerebus.tokens.domain.repository.EffectsRepository

class EffectsRepositoryImpl(private val effectsStorage: EffectsStorage) : EffectsRepository {
    override fun plugWinAnimationOn() {
        effectsStorage.plugWinAnimationOn()
    }

    override fun plugWinAnimationOff() {
        effectsStorage.plugWinAnimationOff()
    }

    override fun plugWinSoundOn() {
        effectsStorage.plugWinSoundOn()
    }

    override fun plugWinSoundOff() {
        effectsStorage.plugWinSoundOff()
    }

    override fun getWinEffects() = WinEffects(
        isWinAnimationOn = effectsStorage.getIsWinAnimation(),
        isWinSoundOn = effectsStorage.getIsWinSound(),
        duration = effectsStorage.getEffectsDuration(),
        animationRepeatTimes = effectsStorage.getAnimationRepeatTimes()
    )
}