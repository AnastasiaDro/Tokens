package com.cerebus.tokens.domain.usecases.tokens

import com.cerebus.tokens.domain.models.Token
import com.cerebus.tokens.domain.repository.TokensRepository
import kotlinx.coroutines.flow.Flow

class GetAllTokensUseCase(private val tokensRepository: com.cerebus.tokens.domain.repository.TokensRepository) {

    fun execute(): Flow<com.cerebus.tokens.domain.models.Token> = tokensRepository.getAllTokens()
}