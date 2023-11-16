package com.cerebus.tokens.domain.repository

import com.cerebus.tokens.domain.models.Token
import kotlinx.coroutines.flow.Flow

interface TokensRepository {

    fun getAllTokens(): Flow<List<Token>>

    fun changeTokensNumber(newNumber: Int)

    fun checkToken(id: Int)

    fun uncheckToken(id: Int)

    fun changeCheckedColor(color: Int)
}