package com.cerebus.tokens.domain.usecases

import com.cerebus.tokens.domain.repository.TokensRepository

class ChangeTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute(newNumber: Int) {
        val currentTokensNumber = tokensRepository.getTokensNumber()

        when {
            newNumber == currentTokensNumber || newNumber <= 0 -> return
            newNumber < currentTokensNumber -> decreaseTokensNumber(currentTokensNumber, newNumber)
            else ->  tokensRepository.createTokens(newNumber - currentTokensNumber)
        }
    }

    private fun decreaseTokensNumber(currentNumber: Int, newNumber: Int) {
        var counts = currentNumber - newNumber

        while (counts > 0) {
            tokensRepository.uncheckToken(currentNumber - 1 - counts)
            counts--
        }
        tokensRepository.removeTokens(currentNumber - newNumber)
    }
}
