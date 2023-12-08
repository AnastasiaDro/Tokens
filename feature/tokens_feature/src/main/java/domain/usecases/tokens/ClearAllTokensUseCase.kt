package domain.usecases.tokens

import domain.repository.TokensRepository

class ClearAllTokensUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute() = tokensRepository.uncheckAllTokens()
}
