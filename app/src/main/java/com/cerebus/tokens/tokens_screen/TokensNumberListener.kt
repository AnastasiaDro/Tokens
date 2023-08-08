package com.cerebus.tokens.tokens_screen

import com.cerebus.tokens.settings_screen.SettingsFragment

/**
 * [TokensNumberListener] a listener for changing number of tokens
 *
 * @see SettingsFragment
 *
 * @author Anastasia Drogunova
 * @since 25.05.2023
 */
interface TokensNumberListener {

    fun changeTokensNumber(newNumber: Int)
}