package presentation.settings_screen

import presentation.state.SettingsSnapshot
import presentation.state.SettingsSnapshotState

/** Pure state transformations; actions and side effects are handled by the ViewModel. */
object SettingsReducer {
    fun SettingsUiState.snapshotObserved(source: SettingsSnapshotState): SettingsUiState {
        val loaded = source.snapshot?.let { snapshotLoaded(it) } ?: this
        return loaded.copy(loading = source.loading, readFailure = source.readFailure)
    }

    fun SettingsUiState.loadingStarted(): SettingsUiState = copy(loading = true)

    fun SettingsUiState.snapshotLoaded(snapshot: SettingsSnapshot): SettingsUiState = copy(
        tokens = TokenSettingsState(snapshot.board.count, snapshot.board.color),
        effects = snapshot.effects, reinforcement = snapshot.reinforcement,
        loading = false, readFailure = false,
    )

    fun SettingsUiState.readFailed(): SettingsUiState = copy(loading = false, readFailure = true)

    fun SettingsUiState.writeStarted(): SettingsUiState = copy(saving = true, writeFailure = false)

    fun SettingsUiState.writeSucceeded(): SettingsUiState = copy(saving = false, writeFailure = false)

    fun SettingsUiState.writeFailed(): SettingsUiState = copy(saving = false, writeFailure = true)
}
