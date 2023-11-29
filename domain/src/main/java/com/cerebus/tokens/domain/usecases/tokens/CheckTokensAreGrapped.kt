package com.cerebus.tokens.domain.usecases.tokens

import com.cerebus.tokens.domain.repository.TokensRepository

class CheckTokensAreGrappedUseCase(private val tokensRepository: com.cerebus.tokens.domain.repository.TokensRepository) {

    fun execute(): Boolean {
        return (tokensRepository.getTokensNumber() == tokensRepository.getCheckedTokensNumber())
    }

}