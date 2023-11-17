package com.cerebus.tokens.domain.usecases.effects

import com.cerebus.tokens.domain.repository.EffectsRepository

class PlugOnOffWinAnimation(private val effectsRepository: EffectsRepository) {

    fun execute(isOn: Boolean) {
        if (isOn)
            effectsRepository.plugWinAnimationOn()
        else
            effectsRepository.plugWinAnimationOff()
    }
}