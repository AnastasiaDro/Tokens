package com.cerebus.tokens.data.storage

/**
 * [TokensStorage] - an interface for storage of tokens data:
 * for example how much tokens in use, wich color is used etc
 *
 * @see TokensStorageImpl
 *
 * @since 18.11.2023
 * @author Anastasia Drogunova
 */
interface TokensStorage {

    fun getTokensNumber(): Int

    fun saveTokensNumber(num: Int)

    fun getCheckedTokensNumber(): Int

    fun saveCheckedTokensNumber(num: Int)

    fun getCheckedTokensColor(): Int

    fun saveCheckedTokensColor(color: Int)

}