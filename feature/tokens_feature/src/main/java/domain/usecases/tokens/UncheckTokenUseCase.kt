package domain.usecases.tokens

import domain.repository.TokensRepository

class UncheckTokenUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute(id: Int) = tokensRepository.uncheckToken(id)
}