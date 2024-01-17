package domain.usecases.tokens

import domain.repository.TokensRepository

class GetMaxTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute() = tokensRepository.getMaxTokensNumber()
}