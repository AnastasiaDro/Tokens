package data.tokens

import data.tokens.storage.TokensStorage

internal class FakeTokensStorage(
    var count: Int = DEFAULT_TOKENS_NUMBER,
    var checkedCount: Int = NO_CHECKED_TOKENS,
    var indices: Set<Int>? = null,
    var color: Int = DEFAULT_COLOR,
) : TokensStorage {
    var writes = NO_WRITES
        private set

    override fun getTokensNumber() = count
    override fun getCheckedTokensNumber() = checkedCount
    override fun getCheckedTokenIndices() = indices
    override fun getCheckedTokensColor() = color
    override fun saveCheckedTokensColor(color: Int) { this.color = color }
    override fun saveProgress(checkedTokens: List<Boolean>) {
        count = checkedTokens.size
        indices = checkedTokens.indices.filter { checkedTokens[it] }.toSet()
        checkedCount = indices!!.size
        writes++
    }

    companion object {
        const val DEFAULT_TOKENS_NUMBER = 5
        const val NO_CHECKED_TOKENS = 0
        private const val DEFAULT_COLOR = -12517557
        private const val NO_WRITES = 0
    }
}
