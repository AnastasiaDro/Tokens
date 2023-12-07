package domain.usecases.tokens

import domain.repository.TokensRepository

class GetCheckedColorUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute() = tokensRepository.getCheckedColor()
}