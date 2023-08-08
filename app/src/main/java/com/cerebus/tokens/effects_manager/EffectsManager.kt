package com.cerebus.tokens.effects_manager

/**
 * [EffectsManager] - provides functions for modifying animation and sound effects
 * when child has sollected all tokens
 *
 * @see EffectsManagerImpl
 * @author Anastasia Drogunova
 * @since 11.07.2023
 */
interface EffectsManager {

    fun setIsAnimateWin(isAnimate: Boolean)
    fun getIsAnimateWin(): Boolean

    fun setIsSoundWin(isSound: Boolean)
    fun getIsSoundWin(): Boolean

    fun isReady(): Boolean

    suspend fun initEffectsManager()
}