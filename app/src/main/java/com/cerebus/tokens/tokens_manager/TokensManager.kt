package com.cerebus.tokens.tokens_manager

/**
 * [TokensManager] - provides functions for modifying tokens parameters
 * Implementations of it can also contain data about tokens parameters
 *
 * @see TokensManagerImpl
 *
 * @author Anastasia Drogunova
 * @since 05.06.2023
 */
interface TokensManager {

    fun setTokensColor(color: Int)
    fun getTokensColor(): Int

    fun setTokensNum(num: Int)
    fun getTokensNum(): Int

    fun getMinTokensNum(): Int
    fun getMaxTokensNum(): Int

    fun setCheckedTokensNumber(num: Int)
    fun getCheckedTokensNumber(): Int
    fun increaseCheckedTokensNum(): Boolean
    fun decreaseCheckedTokensNum(): Boolean

    fun isReady(): Boolean

    suspend fun initTokensManager()
}