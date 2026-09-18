package data.tokens.storage

import android.content.Context
import com.cerebus.tokens.logger.api.LoggerFactory

/**
 * [TokensStorageImpl] - a realisation of [TokensStorage] interface for storage of tokens data
 * Uses SharedPreferences as DataHolder
 *
 * @see TokensStorage
 *
 * @since 18.11.2023
 * @author Anastasia Drogunova
 */
class TokensStorageImpl(context: Context, loggerFactory: LoggerFactory) : TokensStorage {

    private val prefs =  context.getSharedPreferences(TOKENS_PREFERENCES, Context.MODE_PRIVATE)
    private val logger = loggerFactory.createLogger(this::class.java.simpleName)
    override fun getTokensNumber(): Int {
       return prefs.getInt(TOKENS_NUMBER, DEFAULT_TOKENS_NUMBER)
    }

    override fun getCheckedTokensNumber(): Int {
        return prefs.getInt(CHECKED_TOKENS_NUMBER, DEFAULT_CHECKED_TOKENS_NUMBER)
    }

    override fun getCheckedTokenIndices(): Set<Int>? =
        prefs.getStringSet(CHECKED_TOKEN_INDICES, null)?.mapNotNull { it.toIntOrNull() }?.toSet()

    override fun saveProgress(checkedTokens: List<Boolean>) {
        val indices = checkedTokens.indices.filter { checkedTokens[it] }.map { it.toString() }.toSet()
        prefs.edit()
            .putInt(TOKENS_NUMBER, checkedTokens.size)
            .putInt(CHECKED_TOKENS_NUMBER, indices.size)
            .putStringSet(CHECKED_TOKEN_INDICES, indices)
            .apply()
    }

    override fun getCheckedTokensColor(): Int {
        val color = prefs.getInt(CHECKED_TOKENS_COLOR, defaultColor)
        logger.d("get checkedTokensColor = $color")
        return color
    }

    override fun saveCheckedTokensColor(color: Int) {
        logger.d("save checkedTokensColor = $color")
        prefs.edit()?.putInt(CHECKED_TOKENS_COLOR, color)?.apply()
    }

    private companion object {

        const val TOKENS_NUMBER = "TokensNumber"
        const val CHECKED_TOKENS_NUMBER = "CheckedTokensNumber"
        const val CHECKED_TOKEN_INDICES = "CheckedTokenIndices"
        const val CHECKED_TOKENS_COLOR = "CheckedTokensColor"

        private const val DEFAULT_TOKENS_NUMBER = 5
        private const val DEFAULT_CHECKED_TOKENS_NUMBER = 0
        private const val defaultColor = -12517557  /** light green **/
        const val TOKENS_PREFERENCES = "TokensPreferences"
    }
}
