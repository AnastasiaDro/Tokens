package domain.repository

import domain.models.WinEffects

interface EffectsRepository {

    fun plugWinAnimationOn()

    fun plugWinAnimationOff()

    fun plugWinSoundOn()

    fun plugWinSoundOff()

    fun getWinEffects(): WinEffects
}