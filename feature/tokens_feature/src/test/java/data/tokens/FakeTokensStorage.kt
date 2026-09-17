package data.tokens

import data.tokens.storage.TokensStorage

internal class FakeTokensStorage(
    var count: Int = 5,
    var checkedCount: Int = 0,
    var indices: Set<Int>? = null,
    var color: Int = -12517557,
) : TokensStorage {
    var writes = 0
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
}
