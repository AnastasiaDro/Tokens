package domain.usecases.tokens

import domain.repository.TokensRepository

class CheckTokenUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute(id: Int) = tokensRepository.checkToken(id)
}