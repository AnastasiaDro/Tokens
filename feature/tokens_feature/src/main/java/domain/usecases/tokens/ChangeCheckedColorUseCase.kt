package domain.usecases.tokens

import domain.repository.TokensRepository

class ChangeCheckedColorUseCase(private val tokensRepository: TokensRepository) {

    fun execute(color: Int) {
        if (tokensRepository.getCheckedColor() != color) tokensRepository.changeCheckedColor(color)
    }

}