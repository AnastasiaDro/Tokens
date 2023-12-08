package domain.usecases.effects

import domain.repository.EffectsRepository

class PlugOnOffWinSoundUseCase(private val effectsRepository: EffectsRepository) {

    fun execute(isOn: Boolean) {
        if (isOn)
            effectsRepository.plugWinSoundOn()
        else
            effectsRepository.plugWinSoundOff()
    }
}