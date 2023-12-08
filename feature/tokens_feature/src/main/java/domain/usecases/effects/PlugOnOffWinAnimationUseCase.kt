package domain.usecases.effects

class PlugOnOffWinAnimationUseCase(private val effectsRepository: domain.repository.EffectsRepository) {

    fun execute(isOn: Boolean) {
        if (isOn)
            effectsRepository.plugWinAnimationOn()
        else
            effectsRepository.plugWinAnimationOff()
    }
}