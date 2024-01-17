package domain.usecases.tokens

import domain.repository.TokensRepository

class GetTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute() = tokensRepository.getTokensNumber()

}