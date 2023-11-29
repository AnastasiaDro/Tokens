package com.cerebus.tokens.domain.usecases.effects

import com.cerebus.tokens.domain.repository.EffectsRepository

class PlugOnOffWinSound(private val effectsRepository: com.cerebus.tokens.domain.repository.EffectsRepository) {

    fun execute(isOn: Boolean) {
        if (isOn)
            effectsRepository.plugWinSoundOn()
        else
            effectsRepository.plugWinSoundOff()
    }
}