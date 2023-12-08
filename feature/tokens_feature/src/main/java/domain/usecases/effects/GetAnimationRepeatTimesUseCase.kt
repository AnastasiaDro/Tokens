package domain.usecases.effects

import domain.repository.EffectsRepository

class GetAnimationRepeatTimesUseCase(private val effectsRepository: domain.repository.EffectsRepository) {

    fun execute() = effectsRepository.getWinEffects().animationRepeatTimes
}