package domain.usecases.tokens

import domain.repository.TokensRepository

class GetMinTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute() = tokensRepository.getMinTokensNumber()

}