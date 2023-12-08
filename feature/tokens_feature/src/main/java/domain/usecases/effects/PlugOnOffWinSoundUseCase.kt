package domain.usecases.effects

class PlugOnOffWinSoundUseCase(private val effectsRepository: domain.repository.EffectsRepository) {

    fun execute(isOn: Boolean) {
        if (isOn)
            effectsRepository.plugWinSoundOn()
        else
            effectsRepository.plugWinSoundOff()
    }
}