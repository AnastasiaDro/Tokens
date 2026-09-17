package data.tokens.storage

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

    fun getCheckedTokensNumber(): Int

    /** Null denotes a legacy save containing only the number of checked tokens. */
    fun getCheckedTokenIndices(): Set<Int>?

    /** Persist the size, count and exact positions together. */
    fun saveProgress(checkedTokens: List<Boolean>)

    fun getCheckedTokensColor(): Int

    fun saveCheckedTokensColor(color: Int)

}
