package domain.usecases.effects

import domain.repository.EffectsRepository

class IsWinAnimationOnUseCase(private val effectsRepository: domain.repository.EffectsRepository) {

    fun execute() = effectsRepository.getWinEffects().isWinAnimationOn
}