package com.cerebus.tokens.domain.usecases

import com.cerebus.tokens.domain.repository.TokensRepository

class ChangeTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute(newNumber: Int) = tokensRepository.changeTokensNumber(newNumber)
}