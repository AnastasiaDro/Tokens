package domain.usecases.tokens

import domain.repository.TokensRepository

class GetMaxTokensNumberUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute() = tokensRepository.getMaxTokensNumber()
}