package data.tokens.storage

import android.content.Context

/**
 * [TokensStorageImpl] - a realisation of [TokensStorage] interface for storage of tokens data
 * Uses SharedPreferences as DataHolder
 *
 * @see TokensStorage
 *
 * @since 18.11.2023
 * @author Anastasia Drogunova
 */
class TokensStorageImpl(context: Context) : TokensStorage {

    private val prefs =  context.getSharedPreferences(TOKENS_PREFERENCES, Context.MODE_PRIVATE)
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
        const val TOKENS_PREFERENCES = "TokensPreferences"
    }
}