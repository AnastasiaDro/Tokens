package data.tokens

import data.tokens.storage.TokensStorage
import domain.models.Token
import domain.models.resizeTokenProgress
import domain.repository.TokensRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class TokensRepositoryImpl(private val tokensStorage: TokensStorage): TokensRepository {

    private val tokensList = mutableListOf<Token>()

    init {
        val count = tokensStorage.getTokensNumber().coerceIn(getMinTokensNumber(), getMaxTokensNumber())
        val checkedIndices = tokensStorage.getCheckedTokenIndices()
            ?: (FIRST_TOKEN_INDEX until tokensStorage.getCheckedTokensNumber().coerceIn(NO_CHECKED_TOKENS, count)).toSet()
        repeat(count) { index -> tokensList += Token(index in checkedIndices, getCheckedColor()) }
        // Repair legacy count mismatches and migrate to exact positions in one write.
        saveProgress()
    }

    override fun getAllTokens(): Flow<Token> = getTokensList().asFlow()
    override fun getTokensList(): List<Token> = tokensList.toList()

    override fun getTokenById(id: Int) = tokensList[id]
    override fun resizeTokens(number: Int): Boolean {
        if (number !in getMinTokensNumber()..getMaxTokensNumber() || number == tokensList.size) return false
        val progress = resizeTokenProgress(tokensList.map { it.isChecked }, number)
        val color = getCheckedColor()
        val resized = progress.mapIndexed { index, checked ->
            tokensList.getOrNull(index)?.copy(isChecked = checked) ?: Token(checked, color)
        }
        tokensList.clear()
        tokensList.addAll(resized)
        saveProgress()
        return true
    }

    override fun checkToken(id: Int): Boolean {
        val token = tokensList.getOrNull(id) ?: return false
        if (token.isChecked) return false
        tokensList[id] = token.copy(isChecked = true)
        saveProgress()
        return true
    }

    override fun uncheckToken(id: Int): Boolean {
        val token = tokensList.getOrNull(id) ?: return false
        if (!token.isChecked) return false
        tokensList[id] = token.copy(isChecked = false)
        saveProgress()
        return true
    }

    override fun getCheckedColor(): Int = if (tokensStorage.getCheckedTokensColor() == UNSET_COLOR) defaultColor else tokensStorage.getCheckedTokensColor()

    override fun changeCheckedColor(color: Int) {
        tokensStorage.saveCheckedTokensColor(color)
        tokensList.indices.forEach { index -> tokensList[index] = tokensList[index].copy(checkedColor = color) }
    }

    override fun uncheckAllTokens(): Boolean {
        tokensList.indices.forEach { index ->
            tokensList[index] = tokensList[index].copy(isChecked = false)
        }
        saveProgress()
        return true
    }

    override fun getCheckedTokensNumber(): Int {
        return tokensList.count { it.isChecked }
    }

    override fun getTokensNumber() = tokensList.size
    override fun getMinTokensNumber() = MIN_TOKENS_NUMBER

    override fun getMaxTokensNumber() = MAX_TOKENS_NUMBER

    private fun saveProgress() = tokensStorage.saveProgress(tokensList.map { it.isChecked })

    private companion object {
        private const val FIRST_TOKEN_INDEX = 0
        private const val NO_CHECKED_TOKENS = 0
        private const val UNSET_COLOR = 0
        private const val MIN_TOKENS_NUMBER = 1
        private const val MAX_TOKENS_NUMBER = 10
        private const val defaultColor = -12517557  /** light green **/
    }
}
