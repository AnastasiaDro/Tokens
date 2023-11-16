package com.cerebus.tokens.domain.usecases

import com.cerebus.tokens.domain.repository.TokensRepository

class UncheckTokenUseCase(private val tokensRepository: TokensRepository) {

    fun execute(id: Int) = tokensRepository.uncheckToken(id)
}