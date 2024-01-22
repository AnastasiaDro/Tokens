package domain.usecases.tokens

import domain.repository.TokensRepository

class CheckTokenUseCase(private val tokensRepository: TokensRepository) {

    fun execute(id: Int) = tokensRepository.checkToken(id)
}