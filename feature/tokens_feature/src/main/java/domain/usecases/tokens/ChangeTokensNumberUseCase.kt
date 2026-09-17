package domain.usecases.tokens

import domain.repository.TokensRepository

class ChangeTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute(newNumber: Int) {
        tokensRepository.resizeTokens(newNumber)
    }
}
