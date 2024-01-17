package domain.usecases.tokens

import domain.repository.TokensRepository

class CheckTokensAreGrappedUseCase(private val tokensRepository: TokensRepository) {

    fun execute(): Boolean {
        return (tokensRepository.getTokensNumber() == tokensRepository.getCheckedTokensNumber())
    }

}