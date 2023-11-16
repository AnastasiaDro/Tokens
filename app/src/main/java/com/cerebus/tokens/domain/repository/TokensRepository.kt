package com.cerebus.tokens.domain.repository

import com.cerebus.tokens.domain.models.Token
import kotlinx.coroutines.flow.Flow

interface TokensRepository {

    fun getAllTokens(): Flow<Token>

    fun removeTokens(number: Int)

    fun createTokens(number: Int)

    fun checkToken(id: Int)

    fun uncheckToken(id: Int)

    fun getCheckedColor(): Int

    fun changeCheckedColor(color: Int)

    fun uncheckAllTokens()

    fun getTokensNumber(): Int
}