package com.cerebus.tokens.domain.usecases.tokens

import com.cerebus.tokens.domain.repository.TokensRepository

class GetMinTokensNumberUseCase(private val tokensRepository: com.cerebus.tokens.domain.repository.TokensRepository) {

    fun execute() = tokensRepository.getMinTokensNumber()

}