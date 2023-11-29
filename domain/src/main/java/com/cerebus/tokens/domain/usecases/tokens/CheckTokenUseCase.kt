package com.cerebus.tokens.domain.usecases.tokens

import com.cerebus.tokens.domain.repository.TokensRepository

class CheckTokenUseCase(private val tokensRepository: com.cerebus.tokens.domain.repository.TokensRepository) {

    fun execute(id: Int) = tokensRepository.checkToken(id)
}