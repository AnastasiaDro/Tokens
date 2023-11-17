package com.cerebus.tokens.domain.usecases.effects

import com.cerebus.tokens.domain.repository.EffectsRepository

class PlugWinAnimationOffUseCase (private val effectsRepository: EffectsRepository) {

    fun execute() {
        effectsRepository.plugWinAnimationOff()
    }
}