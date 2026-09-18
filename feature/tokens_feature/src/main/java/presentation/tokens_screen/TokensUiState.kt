package presentation.tokens_screen

import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import presentation.state.*

data class TokensUiState(
    val board: BoardState? = null,
    val reinforcement: ReinforcementSettings? = null,
    val effects: WinEffectsState = WinEffectsState(false, false),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: StorageFailure? = null,
)

sealed interface TokensAction {
    data object Loading : TokensAction
    data class Loaded(val snapshot: SettingsSnapshot) : TokensAction
    data class Failed(val failure: StorageFailure) : TokensAction
    data class Saving(val saving: Boolean) : TokensAction
    data class Effects(val effects: WinEffectsState) : TokensAction
}

fun reduceTokens(state: TokensUiState, action: TokensAction): TokensUiState = when (action) {
    TokensAction.Loading -> state.copy(loading = true, error = null)
    is TokensAction.Loaded -> state.copy(board = action.snapshot.board.toState(),
        reinforcement = action.snapshot.reinforcement, loading = false,
        error = state.error?.takeIf { it == StorageFailure.WRITE })
    is TokensAction.Failed -> state.copy(loading = false, saving = false, error = action.failure)
    is TokensAction.Saving -> state.copy(saving = action.saving, error = if (action.saving) null else state.error)
    is TokensAction.Effects -> state.copy(effects = action.effects)
}
