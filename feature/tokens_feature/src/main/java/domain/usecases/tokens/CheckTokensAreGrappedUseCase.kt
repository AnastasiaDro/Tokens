package domain.usecases.tokens

class CheckTokensAreGrappedUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute(): Boolean {
        return (tokensRepository.getTokensNumber() == tokensRepository.getCheckedTokensNumber())
    }

}