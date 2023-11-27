package com.cerebus.tokens.data.storage

interface TokensStorage {

    fun getTokensNumber(): Int

    fun saveTokensNumber(num: Int)

    fun getCheckedTokensNumber(): Int

    fun saveCheckedTokensNumber(num: Int)

    fun getCheckedTokensColor(): Int

    fun saveCheckedTokensColor(color: Int)

}