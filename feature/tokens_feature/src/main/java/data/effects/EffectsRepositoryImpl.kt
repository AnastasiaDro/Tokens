package data.effects

import data.effects.storage.EffectsStorage

class EffectsRepositoryImpl(private val effectsStorage: EffectsStorage) :
    domain.repository.EffectsRepository {
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

    override fun getWinEffects() = domain.models.WinEffects(
        isWinAnimationOn = effectsStorage.getIsWinAnimation(),
        isWinSoundOn = effectsStorage.getIsWinSound(),
        duration = effectsStorage.getEffectsDuration(),
        animationRepeatTimes = effectsStorage.getAnimationRepeatTimes()
    )
}