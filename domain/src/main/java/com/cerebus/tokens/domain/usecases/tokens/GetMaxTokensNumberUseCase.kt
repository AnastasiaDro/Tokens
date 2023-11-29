package com.cerebus.tokens.domain.usecases.tokens

import com.cerebus.tokens.domain.repository.TokensRepository

class GetMaxTokensNumberUseCase(private val tokensRepository: com.cerebus.tokens.domain.repository.TokensRepository) {

    fun execute() = tokensRepository.getMaxTokensNumber()
}