package domain.usecases.tokens

import domain.models.Token
import domain.repository.TokensRepository
import kotlinx.coroutines.flow.Flow

class GetAllTokensUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute(): Flow<domain.models.Token> = tokensRepository.getAllTokens()
}