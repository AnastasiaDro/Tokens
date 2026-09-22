package presentation.tokens_screen

import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import domain.repository.EffectsSettings
import org.junit.Assert.*
import org.junit.Test
import presentation.FakeBoardRepository
import presentation.state.SettingsSnapshot
import presentation.state.SettingsSnapshotState
import presentation.state.toState
import presentation.tokens_screen.TokensReducer.effectsChanged
import presentation.tokens_screen.TokensReducer.loadingStarted
import presentation.tokens_screen.TokensReducer.readFailed
import presentation.tokens_screen.TokensReducer.snapshotLoaded
import presentation.tokens_screen.TokensReducer.snapshotObserved
import presentation.tokens_screen.TokensReducer.writeFailed
import presentation.tokens_screen.TokensReducer.writeStarted
import presentation.tokens_screen.TokensReducer.writeSucceeded

class TokensReducerTest {
    private val board = FakeBoardRepository().values.value
    private val snapshot = SettingsSnapshot(board, EffectsSettings(), ReinforcementSettings(enabled = true))
    private val ready = TokensUiState(board = board.toState(), reinforcement = snapshot.reinforcement, loading = false)

    @Test fun loadedSnapshotDoesNotMutateOriginalAndKeepsWriteFailure() {
        val original = TokensUiState(readFailure = true, writeFailure = true)
        val result = original.snapshotLoaded(snapshot)
        assertEquals(board.toState(), result.board)
        assertEquals(snapshot.reinforcement, result.reinforcement)
        assertFalse(result.loading)
        assertFalse(result.readFailure)
        assertTrue(result.writeFailure)
        assertNull(original.board)
        assertTrue(original.loading)
        assertTrue(original.readFailure)
    }

    @Test fun observedSnapshotUsesSourceReadFlagsWithoutClearingWriteFailure() {
        val original = TokensUiState(writeFailure = true)
        val observed = SettingsSnapshotState(snapshot, loading = true, readFailure = true)
        val result = original.snapshotObserved(observed)
        assertEquals(board.toState(), result.board)
        assertTrue(result.loading)
        assertTrue(result.readFailure)
        assertTrue(result.writeFailure)
        assertNull(original.board)
        val recovered = result.snapshotObserved(SettingsSnapshotState(snapshot, loading = false))
        assertFalse(recovered.loading)
        assertFalse(recovered.readFailure)
        assertTrue(recovered.writeFailure)
    }

    @Test fun missingSnapshotDoesNotErasePreviouslyLoadedData() {
        val result = ready.snapshotObserved(SettingsSnapshotState(loading = false, readFailure = true))
        assertEquals(ready.board, result.board)
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

    @Test fun effectsChangeDoesNotChangeBoardOrAnotherScreenState() {
        val win = WinEffectsState(true, true, CELEBRATION_ID)
        val result = ready.effectsChanged(win)
        assertEquals(ready.copy(effects = win), result)
        assertFalse(ready.effects.isAnimationRunning)
        val other = TokensUiState()
        assertEquals(other.copy(loading = false, readFailure = true), other.readFailed())
        assertEquals(win, result.effects)
    }

    private companion object {
        const val CELEBRATION_ID = 7L
    }
}
