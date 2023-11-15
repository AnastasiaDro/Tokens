package com.cerebus.tokens.tokens_manager

import android.content.SharedPreferences
import androidx.annotation.ColorInt
import com.cerebus.tokens.utils.PrefsConstants
import com.cerebus.tokens.presentation.tokens_screen.TokensViewModel
import com.cerebus.tokens.presentation.settings_screen.SettingsViewModel

/**
 * [TokensManagerImpl] - a realisation of the [TokensManager] interface
 * Intended for containing information about tokens and modifying
 * data about tokens:
 *  - A number of tokens
 *  - A number of checked tokens
 *
 *  It also loads and saves information about tokens to the [SharedPreferences]
 *
 * @see TokensManager
 * @see TokensViewModel
 * @see SettingsViewModel
 *
 * @author Anastasia Drogunova
 * @since 05.06.2023
 */
class TokensManagerImpl(private val prefs: SharedPreferences) : TokensManager {

    private val minTokensNumber = 1
    private val maxTokensNumber = 10
    private var isReady = false

    @ColorInt
    private var checkedTokensColor: Int = -1
    override fun setTokensColor(@ColorInt color: Int) {
        checkedTokensColor = color
        prefs.edit()?.putInt(PrefsConstants.CHECKED_TOKENS_COLOR, color)?.apply()
    }

    override fun getTokensColor() = checkedTokensColor

    private var tokensNum = 1
    override fun setTokensNum(num: Int) {
        tokensNum = num
        prefs.edit()?.putInt(PrefsConstants.TOKENS_NUMBER, tokensNum)?.apply()
    }

    override fun getTokensNum() = tokensNum
    override fun getMinTokensNum() = minTokensNumber

    override fun getMaxTokensNum() = maxTokensNumber

    private var checkedTokensNum = 0
    override fun setCheckedTokensNumber(num: Int) {
        checkedTokensNum = num
        prefs.edit()?.putInt(PrefsConstants.CHECKED_TOKENS_NUMBER, checkedTokensNum)?.apply()
    }

    override fun getCheckedTokensNumber() = checkedTokensNum


    override fun increaseCheckedTokensNum(): Boolean {
        return if (checkedTokensNum < maxTokensNumber) {
            checkedTokensNum++
            prefs.edit()?.putInt(PrefsConstants.CHECKED_TOKENS_NUMBER, checkedTokensNum)?.apply()
            true
        } else false
    }


    override fun decreaseCheckedTokensNum(): Boolean {
        return if (checkedTokensNum >= minTokensNumber) {
            checkedTokensNum--
            prefs.edit()?.putInt(PrefsConstants.CHECKED_TOKENS_NUMBER, checkedTokensNum)?.apply()
            true
        } else false
    }

    override suspend fun initTokensManager() {
        prefs.getInt(PrefsConstants.TOKENS_NUMBER, 5).let { tokensNum = it }
        prefs.getInt(PrefsConstants.CHECKED_TOKENS_NUMBER, 0).let { checkedTokensNum = it }
        prefs.getInt(PrefsConstants.CHECKED_TOKENS_COLOR, defaultColor).let { checkedTokensColor = it }
        isReady = true
    }

    override fun isReady() = isReady

    companion object {
        private const val defaultColor = -12517557  /** light green **/

        private var instance: TokensManager? = null

        fun create(prefs: SharedPreferences): TokensManager? {
            instance = TokensManagerImpl(prefs)
            return instance
        }

        fun getInstance() = instance
    }
}