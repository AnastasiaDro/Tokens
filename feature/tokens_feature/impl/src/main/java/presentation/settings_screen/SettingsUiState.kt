package presentation.settings_screen

import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import domain.repository.EffectsSettings
import presentation.state.*

data class TokenSettingsState(val count: Int, val color: Int)
data class SettingsUiState(
    val tokens: TokenSettingsState? = null,
    val effects: EffectsSettings? = null,
    val reinforcement: ReinforcementSettings? = null,
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

sealed interface SettingsAction {
    data object Loading : SettingsAction
    data class Loaded(val snapshot: SettingsSnapshot) : SettingsAction
    data object ReadFailed : SettingsAction
    data object WriteStarted : SettingsAction
    data object WriteSucceeded : SettingsAction
    data object WriteFailed : SettingsAction
}

fun reduceSettings(state: SettingsUiState, action: SettingsAction): SettingsUiState = when (action) {
    SettingsAction.Loading -> state.copy(loading = true)
    is SettingsAction.Loaded -> state.copy(
        tokens = TokenSettingsState(action.snapshot.board.count, action.snapshot.board.color),
        effects = action.snapshot.effects, reinforcement = action.snapshot.reinforcement,
        loading = false, readFailure = false)
    SettingsAction.ReadFailed -> state.copy(loading = false, readFailure = true)
    SettingsAction.WriteStarted -> state.copy(saving = true, writeFailure = false)
    SettingsAction.WriteSucceeded -> state.copy(saving = false, writeFailure = false)
    SettingsAction.WriteFailed -> state.copy(saving = false, writeFailure = true)
}
