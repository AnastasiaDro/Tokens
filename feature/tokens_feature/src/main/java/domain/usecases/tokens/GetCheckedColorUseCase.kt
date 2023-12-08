package domain.usecases.tokens

import domain.repository.TokensRepository

class GetCheckedColorUseCase(private val tokensRepository: TokensRepository) {

    fun execute() = tokensRepository.getCheckedColor()
}