package domain.usecases.tokens

import domain.repository.TokensRepository

class ClearAllTokensUseCase(private val tokensRepository: TokensRepository) {

    fun execute() = tokensRepository.uncheckAllTokens()
}
