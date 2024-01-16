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
        println("Настя before number= ${tokensRepository.getTokensNumber()}")
        tokensRepository.setTokensNumber(newNumber)
        println("Настя after number= ${tokensRepository.getTokensNumber()}")
    }

    /**
     * decreases tokens and moves checking to the remaining ones
     * @param currentNumber - old number of tokens
     * @param newNumber - target number of tokens
     */
    private fun decreaseTokensNumber(currentNumber: Int, newNumber: Int) {
        val lastIndexOld = currentNumber - 1
        var step = 0

        for (i in lastIndexOld downTo  newNumber) {
            if (tokensRepository.getTokenById(i).isChecked) {
                tokensRepository.uncheckToken(i)

                while (tokensRepository.getTokenById(step).isChecked)
                    step++
                if (step < newNumber) {
                    tokensRepository.checkToken(step)
                    step++
                }
            }
        }
        println("Настя tokensRepositorySize BEFORE = ${tokensRepository.getTokensNumber()}")
        tokensRepository.removeTokens(currentNumber - newNumber)
        println("Настя tokensRepositorySize AFTER = ${tokensRepository.getTokensNumber()}")
    }
}
