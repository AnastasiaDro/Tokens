package domain.usecases.tokens

import domain.repository.TokensRepository

class GetMinTokensNumberUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute() = tokensRepository.getMinTokensNumber()

}