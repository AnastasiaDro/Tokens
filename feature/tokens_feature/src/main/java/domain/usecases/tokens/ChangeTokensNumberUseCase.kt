package domain.usecases.tokens

import domain.repository.TokensRepository

class ChangeTokensNumberUseCase(private val tokensRepository: TokensRepository) {

    fun execute(newNumber: Int) {
        val currentTokensNumber = tokensRepository.getTokensNumber()

        when {
            newNumber == currentTokensNumber || newNumber <= 0 -> return
            newNumber < currentTokensNumber -> decreaseTokensNumber(currentTokensNumber, newNumber)
            else ->  tokensRepository.createTokens(newNumber - currentTokensNumber)
        }
        tokensRepository.setTokensNumber(newNumber)
    }

    /**
     * decreases tokens and moves checking to the remaining ones
     * @param currentNumber - old number of tokens
     * @param newNumber - target number of tokens
     */
    private fun decreaseTokensNumber(currentNumber: Int, newNumber: Int) {
        if (currentNumber != tokensRepository.getCheckedTokensNumber())
            recheck(currentNumber, newNumber)
        else
            uncheck(currentNumber, newNumber)
        tokensRepository.removeTokens(currentNumber - newNumber)
    }

    private fun recheck(currentNumber: Int, newNumber: Int) {
        var step = 0

        for (i in currentNumber - 1 downTo  newNumber) {
            if (tokensRepository.getTokenById(i).isChecked) {
                tokensRepository.uncheckToken(i)

                while (step < tokensRepository.getMaxTokensNumber() && tokensRepository.getTokenById(step).isChecked)
                    step++
                tokensRepository.checkToken(step)
            }
        }
    }

    private fun uncheck(currentNumber: Int, newNumber: Int) {
        for (i in currentNumber - 1 downTo newNumber) {
            if (tokensRepository.getTokenById(i).isChecked) tokensRepository.uncheckToken(i)
        }
    }
}
