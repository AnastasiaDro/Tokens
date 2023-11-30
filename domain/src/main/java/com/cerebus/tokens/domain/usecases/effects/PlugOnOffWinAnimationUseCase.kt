package com.cerebus.tokens.domain.usecases.effects

class PlugOnOffWinAnimationUseCase(private val effectsRepository: com.cerebus.tokens.domain.repository.EffectsRepository) {

    fun execute(isOn: Boolean) {
        if (isOn)
            effectsRepository.plugWinAnimationOn()
        else
            effectsRepository.plugWinAnimationOff()
    }
}