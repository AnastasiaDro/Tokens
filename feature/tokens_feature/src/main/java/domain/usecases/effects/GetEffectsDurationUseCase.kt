package domain.usecases.effects

import domain.repository.EffectsRepository

class GetEffectsDurationUseCase(private val effectsRepository: domain.repository.EffectsRepository) {

    fun execute() = effectsRepository.getWinEffects().duration
}