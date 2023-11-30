package com.cerebus.tokens.domain.usecases.effects

class PlugOnOffWinSoundUseCase(private val effectsRepository: com.cerebus.tokens.domain.repository.EffectsRepository) {

    fun execute(isOn: Boolean) {
        if (isOn)
            effectsRepository.plugWinSoundOn()
        else
            effectsRepository.plugWinSoundOff()
    }
}