package com.cerebus.tokens.domain.usecases.tokens

import com.cerebus.tokens.domain.repository.TokensRepository

class ChangeCheckedColorUseCase(private val tokensRepository: TokensRepository) {

    fun execute(color: Int) {
        if (tokensRepository.getCheckedColor() != color) tokensRepository.changeCheckedColor(color)
    }

}