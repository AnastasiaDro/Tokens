package com.cerebus.tokens.domain.repository

import com.cerebus.tokens.domain.models.WinEffects

interface EffectsRepository {

    fun plugWinAnimationOn()

    fun plugWinAnimationOff()

    fun plugWinSoundOn()

    fun plugWinSoundOff()

    fun getWinEffects(): WinEffects
}