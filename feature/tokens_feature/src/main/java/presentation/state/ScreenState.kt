package presentation.state

import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import domain.repository.*
import kotlinx.coroutines.flow.combine

enum class StorageFailure { READ, WRITE }

data class SettingsSnapshot(
    val board: TokenBoard,
    val effects: EffectsSettings,
    val reinforcement: ReinforcementSettings,
)

fun observeSettings(tokens: TokenBoardRepository, effects: WinEffectsRepository, reinforcement: ReinforcementRepository) =
    combine(tokens.board, effects.settings, reinforcement.settings, ::SettingsSnapshot)

enum class TokenShape { CIRCLE }

data class TokenState(val id: String, val shape: TokenShape, val color: Int, val checked: Boolean)

data class BoardState(val tokens: List<TokenState>, val color: Int, val revision: Long) {
    val count: Int get() = tokens.size
}

fun TokenBoard.toState() = BoardState(tokens.map { TokenState(it.id, TokenShape.CIRCLE, color, it.isChecked) }, color, revision)

