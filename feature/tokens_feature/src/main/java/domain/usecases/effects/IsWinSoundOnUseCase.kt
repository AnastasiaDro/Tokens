package domain.usecases.effects

import domain.repository.EffectsRepository

class IsWinSoundOnUseCase(private val effectsRepository: EffectsRepository) {

    fun execute() = effectsRepository.getWinEffects().isWinSoundOn
}