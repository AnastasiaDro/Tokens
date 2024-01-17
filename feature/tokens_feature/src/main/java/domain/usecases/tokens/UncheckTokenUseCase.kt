package domain.usecases.tokens

import domain.repository.TokensRepository

class UncheckTokenUseCase(private val tokensRepository: TokensRepository) {
    fun execute(id: Int) = tokensRepository.uncheckToken(id)
}