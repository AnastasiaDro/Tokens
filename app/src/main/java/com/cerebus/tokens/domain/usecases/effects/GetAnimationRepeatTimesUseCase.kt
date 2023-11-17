package com.cerebus.tokens.domain.usecases.effects

import com.cerebus.tokens.domain.repository.EffectsRepository

class GetAnimationRepeatTimesUseCase(private val effectsRepository: EffectsRepository) {

    fun execute() = effectsRepository.getWinEffects().animationRepeatTimes
}