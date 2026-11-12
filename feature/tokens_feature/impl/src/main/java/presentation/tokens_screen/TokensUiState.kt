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
    data class TokenClicked(val id: String) : TokensAction
    data object ClearClicked : TokensAction
    data object RetryClicked : TokensAction
    data object SelectCountClicked : TokensAction
    data object SettingsClicked : TokensAction
    data object PhotoClicked : TokensAction
    data class Observed(val source: SettingsSnapshotState) : TokensAction
    data object Loading : TokensAction
    data class Loaded(val snapshot: SettingsSnapshot) : TokensAction
    data object ReadFailed : TokensAction
    data object WriteStarted : TokensAction
    data object WriteSucceeded : TokensAction
    data object WriteFailed : TokensAction
    data class Effects(val effects: WinEffectsState) : TokensAction
}
