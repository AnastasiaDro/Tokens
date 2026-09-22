package presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import domain.repository.MIN_TOKEN_COUNT
import domain.repository.MAX_TOKEN_COUNT
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import presentation.settings_screen.*
import presentation.settings_screen.SettingsReducer.snapshotLoaded
import presentation.settings_screen.SettingsReducer.writeFailed
import presentation.tokens_screen.*
import presentation.tokens_screen.TokensReducer.snapshotLoaded
import presentation.tokens_screen.TokensReducer.writeFailed
import presentation.state.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val tokens = FakeBoardRepository()
    private val effects = FakeEffectsRepository()
    private val reinforcement = FakeReinforcementRepository()
    private lateinit var source: SettingsSnapshotSource
    private fun <T : ViewModel> track(value: T): T = value.also { store.put(java.util.UUID.randomUUID().toString(), it) }
    @Before fun before() {
        Dispatchers.setMain(dispatcher)
        source = SettingsSnapshotSource(tokens, effects, reinforcement, CoroutineScope(SupervisorJob() + dispatcher))
    }
    @After fun after() { store.clear(); source.close(); Dispatchers.resetMain() }

    @Test fun twoScreensAndLateSubscriberSeeSameSavedSettings() = runTest(dispatcher) {
        val board = track(TokensViewModel(tokens, source, TokensNavigator()))
        val settings = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        assertTrue(board.state.value.loading)
        runCurrent()
        val dialog = track(SelectTokensNumberViewModel(tokens, SavedStateHandle()))
        dialog.initialize(SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, FakeBoardRepository.BOARD_SIZE))
        dialog.selectCount(SHRUNK_COUNT)
        dialog.save()
        runCurrent()
        assertEquals(SaveState.SAVED, dialog.state.value.save)
        assertEquals(SHRUNK_COUNT, settings.state.value.tokens?.count)
        assertEquals(SHRUNK_COUNT, board.state.value.board?.count)
        reinforcement.setPhotoUri(PHOTO_URI)
        settings.onAction(SettingsAction.ReinforcementChanged(true))
        settings.onAction(SettingsAction.SoundChanged(false))
        runCurrent()
        val late = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        // No dispatcher advancement: the constructor must use the already loaded snapshot.
        assertEquals(settings.state.value, late.state.value)
        assertEquals(PHOTO_URI, board.state.value.reinforcement?.photoUri)
        assertTrue(board.state.value.reinforcement?.enabled == true)
    }

    @Test fun boardPreloadsSettingsAndClosingViewModelsDoesNotDiscardSnapshot() = runTest(dispatcher) {
        val board = track(TokensViewModel(tokens, source, TokensNavigator()))
        effects.setAnimation(false)
        reinforcement.setEnabled(true)
        runCurrent()
        assertFalse(board.state.value.loading)

        val first = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        assertFalse(first.state.value.loading)
        assertFalse(first.state.value.effects!!.animation)
        assertTrue(first.state.value.effects!!.sound)
        assertTrue(first.state.value.reinforcement!!.enabled)

        store.clear()
        effects.setSound(false)
        runCurrent()
        val reopened = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        assertFalse(reopened.state.value.loading)
        assertFalse(reopened.state.value.effects!!.sound)
        assertTrue(reopened.state.value.reinforcement!!.enabled)
    }

    @Test fun cachedSnapshotWithReadFailureIsImmediatelyVisibleButNotEditable() = runTest(dispatcher) {
        runCurrent()
        val cached = source.state.value.snapshot!!
        effects.failRead = true
        runCurrent()
        val settings = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        assertEquals(cached.effects, settings.state.value.effects)
        assertTrue(settings.state.value.readFailure)
        settings.onAction(SettingsAction.SoundChanged(false))
        runCurrent()
        assertEquals(FakeBoardRepository.NO_WRITES, effects.writeAttempts)
        effects.failRead = false
        settings.onAction(SettingsAction.RetryClicked)
        assertTrue(settings.state.value.loading)
        assertTrue(settings.state.value.readFailure)
        assertEquals(cached.effects, settings.state.value.effects)
        runCurrent()
        assertFalse(settings.state.value.readFailure)
        assertFalse(settings.state.value.loading)
    }

    @Test fun readFailureIsNotAnEmptyBoardAndCanRetry() = runTest(dispatcher) {
        tokens.failRead = true
        val vm = track(TokensViewModel(tokens, source, TokensNavigator()))
        runCurrent()
        assertNull(vm.state.value.board)
        assertEquals(StorageFailure.READ, vm.state.value.error)
        tokens.failRead = false
        vm.onAction(TokensAction.RetryClicked)
        runCurrent()
        assertEquals(FakeBoardRepository.BOARD_SIZE, vm.state.value.board?.count)
        assertNull(vm.state.value.error)
    }

    @Test fun failedSettingsWriteKeepsSavedValueAndRetries() = runTest(dispatcher) {
        val vm = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        runCurrent()
        effects.failWrite = true
        vm.onAction(SettingsAction.SoundChanged(false))
        runCurrent()
        assertTrue(vm.state.value.effects!!.sound)
        assertEquals(StorageFailure.WRITE, vm.state.value.error)
        effects.failWrite = false
        vm.onAction(SettingsAction.RetryClicked)
        runCurrent()
        assertFalse(vm.state.value.effects!!.sound)
        assertNull(vm.state.value.error)
    }

    @Test fun countDialogOnlyCompletesAfterSuccessfulWrite() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        tokens.beforeWrite = { gate.await() }
        val vm = track(SelectTokensNumberViewModel(tokens, SavedStateHandle()))
        vm.initialize(SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, FakeBoardRepository.BOARD_SIZE))
        vm.selectCount(SHRUNK_COUNT)
        vm.save()
        runCurrent()
        assertEquals(SaveState.SAVING, vm.state.value.save)
        tokens.failWrite = true
        gate.complete(Unit)
        runCurrent()
        assertEquals(SaveState.ERROR, vm.state.value.save)
        assertEquals(FakeBoardRepository.BOARD_SIZE, tokens.values.value.count)
        tokens.failWrite = false
        vm.save()
        runCurrent()
        assertEquals(SaveState.SAVED, vm.state.value.save)
    }

    @Test fun colorHasItsOwnViewModelAndOnlyCompletesAfterWrite() = runTest(dispatcher) {
        val vm = track(SelectColorViewModel(tokens, androidx.lifecycle.SavedStateHandle()))
        runCurrent()
        tokens.failWrite = true
        vm.selectColor(COLOR)
        vm.save()
        runCurrent()
        assertEquals(SaveState.ERROR, vm.state.value.save)
        assertEquals(COLOR, vm.state.value.color)
        assertEquals(FakeBoardRepository.COLOR, tokens.values.value.color)
        tokens.failWrite = false
        vm.save()
        runCurrent()
        assertEquals(SaveState.SAVED, vm.state.value.save)
        assertEquals(COLOR, vm.state.value.color)
    }

    @Test fun reducersPreserveSavedDataOnFailureWithoutMutatingOldState() {
        val initial = TokensUiState()
        val snapshot = SettingsSnapshot(tokens.values.value, effects.values.value, reinforcement.settings.value)
        val loaded = initial.snapshotLoaded(snapshot)
        val failed = loaded.writeFailed()
        assertNull(initial.board)
        assertEquals(loaded.board, failed.board)
        assertNull(loaded.error)
        val settings = SettingsUiState().snapshotLoaded(snapshot)
        assertEquals(settings.tokens, settings.writeFailed().tokens)
    }

    @Test fun readRecoveryRetriesFailedSettingsWriteOnceAndKeepsObserving() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        effects.beforeWrite = { gate.await() }
        val vm = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        runCurrent()

        vm.onAction(SettingsAction.SoundChanged(false))
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
        vm.onAction(SettingsAction.RetryClicked)
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
        val vm = track(SettingsViewModel(source, effects, reinforcement, SettingsNavigator()))
        runCurrent()

        vm.onAction(SettingsAction.SoundChanged(false))
        runCurrent()
        effects.failRead = true
        runCurrent()
        effects.failRead = false
        vm.onAction(SettingsAction.RetryClicked)
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
