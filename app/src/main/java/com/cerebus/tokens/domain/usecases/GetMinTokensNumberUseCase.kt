package com.cerebus.tokens.domain.usecases

import com.cerebus.tokens.domain.repository.TokensRepository

class GetMinTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute() = tokensRepository.getMinTokensNumber()

}