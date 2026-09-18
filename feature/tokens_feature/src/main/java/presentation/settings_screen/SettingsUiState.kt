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
    val error: StorageFailure? = null,
)

sealed interface SettingsAction {
    data object Loading : SettingsAction
    data class Loaded(val snapshot: SettingsSnapshot) : SettingsAction
    data class Failed(val failure: StorageFailure) : SettingsAction
    data class Saving(val saving: Boolean) : SettingsAction
}

fun reduceSettings(state: SettingsUiState, action: SettingsAction): SettingsUiState = when (action) {
    SettingsAction.Loading -> state.copy(loading = true, error = null)
    is SettingsAction.Loaded -> state.copy(
        tokens = TokenSettingsState(action.snapshot.board.count, action.snapshot.board.color),
        effects = action.snapshot.effects, reinforcement = action.snapshot.reinforcement, loading = false,
        error = state.error?.takeIf { it == StorageFailure.WRITE })
    is SettingsAction.Failed -> state.copy(loading = false, saving = false, error = action.failure)
    is SettingsAction.Saving -> state.copy(saving = action.saving, error = if (action.saving) null else state.error)
}

