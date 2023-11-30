package com.cerebus.tokens.domain.usecases.tokens

class CheckTokensAreGrappedUseCase(private val tokensRepository: com.cerebus.tokens.domain.repository.TokensRepository) {

    fun execute(): Boolean {
        return (tokensRepository.getTokensNumber() == tokensRepository.getCheckedTokensNumber())
    }

}