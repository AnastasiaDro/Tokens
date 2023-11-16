package com.cerebus.tokens.data

import android.content.Context
import android.content.SharedPreferences
import com.cerebus.tokens.domain.models.Token
import com.cerebus.tokens.domain.repository.TokensRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

//class TokensRepositoryImpl(context: Context): TokensRepository {
class TokensRepositoryImpl(private val prefs: SharedPreferences): TokensRepository {

   // private val prefs = context.getSharedPreferences(TOKENS_PREFERENCES, Context.MODE_PRIVATE)
    private val tokensList = mutableListOf<Token>()
    private var checkedColor = prefs.getInt(CHECKED_TOKENS_COLOR, defaultColor)

    init {
        createTokens(prefs.getInt(TOKENS_NUMBER, 5))
        checkTokens(prefs.getInt(CHECKED_TOKENS_NUMBER, 0))
    }

    override fun getAllTokens(): Flow<Token> = tokensList.asFlow()
    override fun createTokens(number: Int) {
        for (i in 0 until number)
            tokensList += Token(false)
    }

    override fun removeTokens(number: Int) {
        tokensList.dropLast(number)
    }

    override fun checkToken(id: Int): Boolean {
        tokensList[id].isChecked = true
        var checkedTokensNumber = prefs.getInt(CHECKED_TOKENS_NUMBER, 0)
        checkedTokensNumber++
        prefs.edit()?.putInt(CHECKED_TOKENS_NUMBER, checkedTokensNumber)?.apply()
        return true
    }

    override fun uncheckToken(id: Int): Boolean {
        tokensList[id].isChecked = false
        var checkedTokensNumber = prefs.getInt(CHECKED_TOKENS_NUMBER, 0)
        checkedTokensNumber--
        prefs.edit()?.putInt(CHECKED_TOKENS_NUMBER, checkedTokensNumber)?.apply()
        return true
    }

    override fun getCheckedColor(): Int = checkedColor

    override fun changeCheckedColor(color: Int) {
        checkedColor = color
        prefs.edit()?.putInt(CHECKED_TOKENS_COLOR, color)?.apply()
    }

    override fun uncheckAllTokens(): Boolean {
        tokensList.forEach {
            it.isChecked = false
        }
        return true
    }

    override fun getCheckedTokensNumber(): Int {
        return prefs.getInt(CHECKED_TOKENS_NUMBER, 0)
    }

    override fun getTokensNumber() = tokensList.size
    override fun getMinTokensNumber() = 1

    override fun getMaxTokensNumber() = 10

    private fun checkTokens(num: Int) {
        for (i in 0 until num) {
            tokensList[i].isChecked = true
        }
    }

    private companion object {
        const val TOKENS_PREFERENCES = "TokensPreferences"
        const val TOKENS_NUMBER = "TokensNumber"
        const val CHECKED_TOKENS_NUMBER = "CheckedTokensNumber"
        const val CHECKED_TOKENS_COLOR = "CheckedTokensColor"

        private const val defaultColor = -12517557  /** light green **/
    }
}