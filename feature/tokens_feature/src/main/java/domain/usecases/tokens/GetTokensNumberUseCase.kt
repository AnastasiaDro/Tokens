package domain.usecases.tokens

import domain.repository.TokensRepository

class GetTokensNumberUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute() = tokensRepository.getTokensNumber()

}