package domain.usecases.effects

import domain.repository.EffectsRepository

class IsWinSoundOnUseCase(private val effectsRepository: domain.repository.EffectsRepository) {

    fun execute() = effectsRepository.getWinEffects().isWinSoundOn
}