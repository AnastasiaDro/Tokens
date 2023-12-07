package domain.usecases.tokens

import domain.repository.TokensRepository

class ChangeCheckedColorUseCase(private val tokensRepository: domain.repository.TokensRepository) {

    fun execute(color: Int) {
        if (tokensRepository.getCheckedColor() != color) tokensRepository.changeCheckedColor(color)
    }

}