package domain.usecases.tokens

import domain.models.Token
import domain.repository.TokensRepository
import kotlinx.coroutines.flow.Flow

class GetAllTokensUseCase(private val tokensRepository: TokensRepository) {

    fun execute(): Flow<Token> = tokensRepository.getAllTokens()
}