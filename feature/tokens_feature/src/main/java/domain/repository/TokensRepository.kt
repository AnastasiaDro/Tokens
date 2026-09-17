package domain.repository

import domain.models.Token
import kotlinx.coroutines.flow.Flow

interface TokensRepository {

    fun getAllTokens(): Flow<Token>

    //tmp
    fun getTokensList(): List<Token>

    fun getTokenById(id: Int): Token

    fun resizeTokens(number: Int): Boolean

    fun checkToken(id: Int): Boolean

    fun uncheckToken(id: Int): Boolean

    fun getCheckedColor(): Int

    fun changeCheckedColor(color: Int)

    fun uncheckAllTokens(): Boolean

    fun getTokensNumber(): Int

    fun getCheckedTokensNumber(): Int

    fun getMinTokensNumber(): Int

    fun getMaxTokensNumber(): Int
}
