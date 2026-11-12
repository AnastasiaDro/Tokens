package presentation.settings_screen

import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import domain.repository.EffectsSettings
import org.junit.Assert.*
import org.junit.Test
import presentation.FakeBoardRepository
import presentation.state.SettingsSnapshot
import presentation.state.SettingsSnapshotState
import presentation.settings_screen.SettingsReducer.loadingStarted
import presentation.settings_screen.SettingsReducer.readFailed
import presentation.settings_screen.SettingsReducer.snapshotLoaded
import presentation.settings_screen.SettingsReducer.snapshotObserved
import presentation.settings_screen.SettingsReducer.writeFailed
import presentation.settings_screen.SettingsReducer.writeStarted
import presentation.settings_screen.SettingsReducer.writeSucceeded

class SettingsReducerTest {
    private val board = FakeBoardRepository().values.value
    private val snapshot = SettingsSnapshot(board, EffectsSettings(), ReinforcementSettings(enabled = true))
    private val ready = SettingsUiState().snapshotLoaded(snapshot)

    @Test fun loadedSnapshotDoesNotMutateOriginalAndKeepsWriteFailure() {
        val original = SettingsUiState(readFailure = true, writeFailure = true)
        val result = original.snapshotLoaded(snapshot)
        assertEquals(TokenSettingsState(board.count, board.color), result.tokens)
        assertEquals(snapshot.reinforcement, result.reinforcement)
        assertFalse(result.loading)
        assertFalse(result.readFailure)
        assertTrue(result.writeFailure)
        assertNull(original.tokens)
        assertTrue(original.loading)
        assertTrue(original.readFailure)
    }

    @Test fun observedSnapshotUsesSourceReadFlagsWithoutClearingWriteFailure() {
        val original = SettingsUiState(writeFailure = true)
        val observed = SettingsSnapshotState(snapshot, loading = true, readFailure = true)
        val result = original.snapshotObserved(observed)
        assertEquals(TokenSettingsState(board.count, board.color), result.tokens)
        assertTrue(result.loading)
        assertTrue(result.readFailure)
        assertTrue(result.writeFailure)
        assertNull(original.tokens)
        val recovered = result.snapshotObserved(SettingsSnapshotState(snapshot, loading = false))
        assertFalse(recovered.loading)
        assertFalse(recovered.readFailure)
        assertTrue(recovered.writeFailure)
    }

    @Test fun missingSnapshotDoesNotErasePreviouslyLoadedData() {
        val result = ready.snapshotObserved(SettingsSnapshotState(loading = false, readFailure = true))
        assertEquals(ready.tokens, result.tokens)
        assertEquals(ready.reinforcement, result.reinforcement)
        assertTrue(result.readFailure)
        assertFalse(ready.readFailure)
    }

    @Test fun loadingAndReadFailurePreserveSavedDataAndWriteStatus() {
        val original = ready.copy(saving = true, writeFailure = true)
        val loading = original.loadingStarted()
        assertEquals(original.copy(loading = true), loading)
        assertEquals(original.copy(readFailure = true), loading.readFailed())
        assertFalse(original.loading)
        assertFalse(original.readFailure)
    }

    @Test fun writeTransitionsChangeOnlyWriteStatusAndPreserveReadFailure() {
        val original = ready.copy(readFailure = true, writeFailure = true)
        val started = original.writeStarted()
        assertEquals(original.copy(saving = true, writeFailure = false), started)
        val failed = started.writeFailed()
        assertEquals(original, failed)
        val saved = failed.writeStarted().writeSucceeded()
        assertEquals(original.copy(writeFailure = false), saved)
        assertTrue(original.writeFailure)
        assertFalse(original.saving)
    }

}
