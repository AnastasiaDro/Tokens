package com.cerebus.tokens.data.storage

import android.content.SharedPreferences

class TokensStorageImpl(private val prefs: SharedPreferences) : TokensStorage {
    override fun getTokensNumber(): Int {
       return prefs.getInt(TOKENS_NUMBER, 1)
    }

    override fun saveTokensNumber(num: Int) {
        prefs.edit().putInt(TOKENS_NUMBER, num).apply()
    }

    override fun getCheckedTokensNumber(): Int {
        return prefs.getInt(CHECKED_TOKENS_NUMBER, 0)
    }

    override fun saveCheckedTokensNumber(num: Int) {
        prefs.edit().putInt(CHECKED_TOKENS_NUMBER, num).apply()
    }

    override fun getCheckedTokensColor(): Int {
        return prefs.getInt(CHECKED_TOKENS_COLOR, 0)
    }

    override fun saveCheckedTokensColor(color: Int) {
        prefs.edit()?.putInt(CHECKED_TOKENS_COLOR, color)?.apply()
    }

    private companion object {
        const val TAG = "TokensStorage"

        const val TOKENS_NUMBER = "TokensNumber"
        const val CHECKED_TOKENS_NUMBER = "CheckedTokensNumber"
        const val CHECKED_TOKENS_COLOR = "CheckedTokensColor"

        private const val defaultColor = -12517557  /** light green **/
    }
}