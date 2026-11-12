package presentation.tokens_screen

import presentation.state.SettingsSnapshot
import presentation.state.SettingsSnapshotState
import presentation.state.toState

/** Pure state transformations; actions and side effects are handled by the ViewModel. */
object TokensReducer {
    fun TokensUiState.snapshotObserved(source: SettingsSnapshotState): TokensUiState {
        val loaded = source.snapshot?.let { snapshotLoaded(it) } ?: this
        return loaded.copy(loading = source.loading, readFailure = source.readFailure)
    }

    fun TokensUiState.loadingStarted(): TokensUiState = copy(loading = true)

    fun TokensUiState.snapshotLoaded(snapshot: SettingsSnapshot): TokensUiState =
        copy(board = snapshot.board.toState(), reinforcement = snapshot.reinforcement,
            loading = false, readFailure = false)

    fun TokensUiState.readFailed(): TokensUiState = copy(loading = false, readFailure = true)

    fun TokensUiState.writeStarted(): TokensUiState = copy(saving = true, writeFailure = false)

    fun TokensUiState.writeSucceeded(): TokensUiState = copy(saving = false, writeFailure = false)

    fun TokensUiState.writeFailed(): TokensUiState = copy(saving = false, writeFailure = true)

    fun TokensUiState.effectsChanged(effects: WinEffectsState): TokensUiState = copy(effects = effects)
}
