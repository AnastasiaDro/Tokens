package presentation.tokens_screen

import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import presentation.state.*

data class TokensUiState(
    val board: BoardState? = null,
    val reinforcement: ReinforcementSettings? = null,
    val effects: WinEffectsState = WinEffectsState(false, false),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val readFailure: Boolean = false,
    val writeFailure: Boolean = false,
) {
    val error: StorageFailure?
        get() = when {
            readFailure -> StorageFailure.READ
            writeFailure -> StorageFailure.WRITE
            else -> null
        }
}

sealed interface TokensAction {
    data object Loading : TokensAction
    data class Loaded(val snapshot: SettingsSnapshot) : TokensAction
    data object ReadFailed : TokensAction
    data object WriteStarted : TokensAction
    data object WriteSucceeded : TokensAction
    data object WriteFailed : TokensAction
    data class Effects(val effects: WinEffectsState) : TokensAction
}

fun reduceTokens(state: TokensUiState, action: TokensAction): TokensUiState = when (action) {
    TokensAction.Loading -> state.copy(loading = true)
    is TokensAction.Loaded -> state.copy(board = action.snapshot.board.toState(),
        reinforcement = action.snapshot.reinforcement, loading = false, readFailure = false)
    TokensAction.ReadFailed -> state.copy(loading = false, readFailure = true)
    TokensAction.WriteStarted -> state.copy(saving = true, writeFailure = false)
    TokensAction.WriteSucceeded -> state.copy(saving = false, writeFailure = false)
    TokensAction.WriteFailed -> state.copy(saving = false, writeFailure = true)
    is TokensAction.Effects -> state.copy(effects = action.effects)
}
