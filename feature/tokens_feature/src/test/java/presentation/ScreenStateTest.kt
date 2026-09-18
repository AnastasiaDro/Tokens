package presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import presentation.settings_screen.*
import presentation.tokens_screen.*
import presentation.state.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val tokens = FakeBoardRepository()
    private val effects = FakeEffectsRepository()
    private val reinforcement = FakeReinforcementRepository()
    private fun <T : ViewModel> track(value: T): T = value.also { store.put(java.util.UUID.randomUUID().toString(), it) }
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }

    @Test fun twoScreensAndLateSubscriberSeeSameSavedSettings() = runTest(dispatcher) {
        val board = track(TokensViewModel(tokens, effects, reinforcement))
        val settings = track(SettingsViewModel(tokens, effects, reinforcement))
        assertTrue(board.state.value.loading)
        runCurrent()
        val dialog = track(SelectTokensNumberViewModel(tokens))
        dialog.changeTokensNum(SHRUNK_COUNT)
        runCurrent()
        assertEquals(SaveState.SAVED, dialog.state.value)
        assertEquals(SHRUNK_COUNT, settings.state.value.tokens?.count)
        assertEquals(SHRUNK_COUNT, board.state.value.board?.count)
        reinforcement.setPhotoUri(PHOTO_URI)
        settings.changeReinforcement(true)
        settings.changeSound(false)
        runCurrent()
        val late = track(SettingsViewModel(tokens, effects, reinforcement))
        runCurrent()
        assertEquals(settings.state.value, late.state.value)
        assertEquals(PHOTO_URI, board.state.value.reinforcement?.photoUri)
        assertTrue(board.state.value.reinforcement?.enabled == true)
    }

    @Test fun readFailureIsNotAnEmptyBoardAndCanRetry() = runTest(dispatcher) {
        tokens.failRead = true
        val vm = track(TokensViewModel(tokens, effects, reinforcement))
        runCurrent()
        assertNull(vm.state.value.board)
        assertEquals(StorageFailure.READ, vm.state.value.error)
        tokens.failRead = false
        vm.retry()
        runCurrent()
        assertEquals(FakeBoardRepository.BOARD_SIZE, vm.state.value.board?.count)
        assertNull(vm.state.value.error)
    }

    @Test fun failedSettingsWriteKeepsSavedValueAndRetries() = runTest(dispatcher) {
        val vm = track(SettingsViewModel(tokens, effects, reinforcement))
        runCurrent()
        effects.failWrite = true
        vm.changeSound(false)
        runCurrent()
        assertTrue(vm.state.value.effects!!.sound)
        assertEquals(StorageFailure.WRITE, vm.state.value.error)
        effects.failWrite = false
        vm.retry()
        runCurrent()
        assertFalse(vm.state.value.effects!!.sound)
        assertNull(vm.state.value.error)
    }

    @Test fun countDialogOnlyCompletesAfterSuccessfulWrite() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        tokens.beforeWrite = { gate.await() }
        val vm = track(SelectTokensNumberViewModel(tokens))
        vm.changeTokensNum(SHRUNK_COUNT)
        runCurrent()
        assertEquals(SaveState.SAVING, vm.state.value)
        tokens.failWrite = true
        gate.complete(Unit)
        runCurrent()
        assertEquals(SaveState.ERROR, vm.state.value)
        assertEquals(FakeBoardRepository.BOARD_SIZE, tokens.values.value.count)
        tokens.failWrite = false
        vm.changeTokensNum(SHRUNK_COUNT)
        runCurrent()
        assertEquals(SaveState.SAVED, vm.state.value)
    }

    @Test fun colorHasItsOwnViewModelAndOnlyCompletesAfterWrite() = runTest(dispatcher) {
        val vm = track(SelectColorViewModel(tokens))
        runCurrent()
        tokens.failWrite = true
        vm.save(COLOR)
        runCurrent()
        assertEquals(SaveState.ERROR, vm.state.value.save)
        assertEquals(FakeBoardRepository.COLOR, vm.state.value.color)
        tokens.failWrite = false
        vm.save(COLOR)
        runCurrent()
        assertEquals(SaveState.SAVED, vm.state.value.save)
        assertEquals(COLOR, vm.state.value.color)
    }

    @Test fun reducersPreserveSavedDataOnFailureWithoutMutatingOldState() {
        val initial = TokensUiState()
        val snapshot = SettingsSnapshot(tokens.values.value, effects.values.value, reinforcement.settings.value)
        val loaded = reduceTokens(initial, TokensAction.Loaded(snapshot))
        val failed = reduceTokens(loaded, TokensAction.WriteFailed)
        assertNull(initial.board)
        assertEquals(loaded.board, failed.board)
        assertNull(loaded.error)
        val settings = reduceSettings(SettingsUiState(), SettingsAction.Loaded(snapshot))
        assertEquals(settings.tokens, reduceSettings(settings, SettingsAction.WriteFailed).tokens)
    }

    @Test fun readRecoveryRetriesFailedSettingsWriteOnceAndKeepsObserving() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        effects.beforeWrite = { gate.await() }
        val vm = track(SettingsViewModel(tokens, effects, reinforcement))
        runCurrent()

        vm.changeSound(false)
        runCurrent()
        effects.failRead = true
        runCurrent()
        assertTrue(vm.state.value.readFailure)
        assertTrue(vm.state.value.saving)

        effects.failWrite = true
        gate.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.readFailure)
        assertTrue(vm.state.value.writeFailure)
        assertEquals(StorageFailure.READ, vm.state.value.error)

        effects.beforeWrite = {}
        effects.failRead = false
        effects.failWrite = false
        vm.retry()
        runCurrent()

        assertFalse(effects.values.value.sound)
        assertFalse(vm.state.value.effects!!.sound)
        assertFalse(vm.state.value.readFailure)
        assertFalse(vm.state.value.writeFailure)
        assertEquals(EXPECTED_RETRY_ATTEMPTS, effects.writeAttempts)
        assertEquals(EXPECTED_SUCCESSFUL_WRITES, effects.successfulWrites)

        effects.setAnimation(false)
        runCurrent()
        assertFalse(vm.state.value.effects!!.animation)
    }

    @Test fun readRetryDoesNotDuplicateWriteStillInProgress() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        effects.beforeWrite = { gate.await() }
        val vm = track(SettingsViewModel(tokens, effects, reinforcement))
        runCurrent()

        vm.changeSound(false)
        runCurrent()
        effects.failRead = true
        runCurrent()
        effects.failRead = false
        vm.retry()
        runCurrent()

        gate.complete(Unit)
        runCurrent()
        assertFalse(effects.values.value.sound)
        assertFalse(vm.state.value.effects!!.sound)
        assertEquals(EXPECTED_SINGLE_ATTEMPT, effects.writeAttempts)
        assertEquals(EXPECTED_SUCCESSFUL_WRITES, effects.successfulWrites)
    }

    private companion object {
        const val SHRUNK_COUNT = 1
        const val COLOR = -65536
        const val PHOTO_URI = "content://test/photo"
        const val EXPECTED_SINGLE_ATTEMPT = 1
        const val EXPECTED_RETRY_ATTEMPTS = 2
        const val EXPECTED_SUCCESSFUL_WRITES = 1
    }
}
