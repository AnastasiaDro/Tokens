package data.tokens

import android.util.Log
import data.tokens.storage.TokensStorage
import domain.models.Token
import domain.repository.TokensRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class TokensRepositoryImpl(private val tokensStorage: TokensStorage): TokensRepository {

    private val tokensList = mutableListOf<Token>()

    init {
        createTokens(tokensStorage.getTokensNumber())
        checkTokens(tokensStorage.getCheckedTokensNumber())
    }

    override fun getAllTokens(): Flow<Token> = tokensList.asFlow()

    override fun getTokenById(id: Int) = tokensList[id]
    override fun createTokens(number: Int) {
        for (i in 0 until number)
            tokensList += Token(false)

    }

    override fun removeTokens(number: Int) {
        repeat(number) { tokensList.removeLast() }
    }

    override fun checkToken(id: Int): Boolean {
        tokensList[id].isChecked = true
        var checkedTokensNumber = tokensStorage.getCheckedTokensNumber()
        checkedTokensNumber++
        tokensStorage.saveCheckedTokensNumber(checkedTokensNumber)
        Log.d(TAG, "Token $id was checked")
        return true
    }

    override fun uncheckToken(id: Int): Boolean {
        tokensList[id].isChecked = false
        var checkedTokensNumber = tokensStorage.getCheckedTokensNumber()
        checkedTokensNumber--
        tokensStorage.saveCheckedTokensNumber(checkedTokensNumber)
        Log.d(TAG, "Token $id was unchecked")
        return true
    }

    override fun getCheckedColor(): Int = if (tokensStorage.getCheckedTokensColor() == 0) defaultColor else tokensStorage.getCheckedTokensColor()

    override fun changeCheckedColor(color: Int) {
        tokensStorage.saveCheckedTokensColor(color)
    }

    override fun uncheckAllTokens(): Boolean {
        tokensList.forEach {
            it.isChecked = false
        }
        tokensStorage.saveCheckedTokensNumber(0)
        return true
    }

    override fun getCheckedTokensNumber(): Int {
        return tokensStorage.getCheckedTokensNumber()
    }

    override fun getTokensNumber() = tokensStorage.getTokensNumber()

    override fun setTokensNumber(num: Int) {
        tokensStorage.saveTokensNumber(num)
    }
    override fun getMinTokensNumber() = 1

    override fun getMaxTokensNumber() = 10

    private fun checkTokens(num: Int) {
        for (i in 0 until num) {
            tokensList[i].isChecked = true
        }
        Log.d(TAG, "$num tokens marked as checked")
    }

    private companion object {
        const val TAG = "TokensRepository"

        private const val defaultColor = -12517557  /** light green **/
    }
}