package domain.repository

import domain.models.Token
import kotlinx.coroutines.flow.Flow

const val MIN_TOKEN_COUNT = 1
const val MAX_TOKEN_COUNT = 20
const val INITIAL_BOARD_REVISION = 0L

data class TokenBoard(val tokens: List<Token>, val color: Int, val revision: Long) {
    val count: Int get() = tokens.size
    val completed: Boolean get() = tokens.isNotEmpty() && tokens.all { it.isChecked }
}

data class TokenChange(val board: TokenBoard, val changed: Boolean, val completedByUser: Boolean)

interface TokenBoardRepository {
    val board: Flow<TokenBoard>
    suspend fun toggle(id: String): TokenChange
    suspend fun setChecked(id: String, checked: Boolean): TokenChange
    suspend fun resize(count: Int)
    suspend fun setColor(color: Int)
    suspend fun clear()
}
