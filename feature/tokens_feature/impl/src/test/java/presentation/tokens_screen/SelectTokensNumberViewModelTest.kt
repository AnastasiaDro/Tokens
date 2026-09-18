package presentation.tokens_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import presentation.FakeBoardRepository
import presentation.SelectTokensNumberAlertData
import presentation.state.SaveState
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SelectTokensNumberViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val tokens = FakeBoardRepository()
    private val args = SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, FakeBoardRepository.BOARD_SIZE)

    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        SelectTokensNumberViewModel(tokens, handle).also { store.put(UUID.randomUUID().toString(), it) }

    @Test fun draftDoesNotWriteAndInitializationDoesNotResetIt() = runTest(dispatcher) {
        val vm = viewModel()
        vm.initialize(args)
        vm.selectCount(SELECTED_COUNT)
        vm.initialize(args)
        runCurrent()
        assertEquals(SELECTED_COUNT, vm.state.value.count)
        assertEquals(FakeBoardRepository.BOARD_SIZE, tokens.values.value.count)
        assertEquals(SaveState.IDLE, vm.state.value.save)
    }

    @Test fun draftRestoresIntoNewViewModelWithoutAutomaticWrite() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.initialize(args)
        vm.selectCount(SELECTED_COUNT)
        val restored = viewModel(copy(handle))
        restored.initialize(args)
        runCurrent()
        assertEquals(SELECTED_COUNT, restored.state.value.count)
        assertTrue(restored.state.value.editable)
        assertEquals(FakeBoardRepository.BOARD_SIZE, tokens.values.value.count)
    }

    @Test fun duplicateConfirmAndEditingAreBlockedUntilWriteCompletes() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val writes = mutableListOf<Unit>()
        tokens.beforeWrite = { writes += Unit; gate.await() }
        val vm = viewModel()
        vm.initialize(args)
        vm.selectCount(SELECTED_COUNT)
        vm.save()
        vm.save()
        vm.selectCount(MIN_TOKEN_COUNT)
        runCurrent()
        assertEquals(listOf(Unit), writes)
        assertEquals(SELECTED_COUNT, vm.state.value.count)
        assertFalse(vm.state.value.editable)
        gate.complete(Unit)
        runCurrent()
        vm.save()
        runCurrent()
        assertEquals(listOf(Unit), writes)
        assertEquals(SaveState.SAVED, vm.state.value.save)
        assertEquals(SELECTED_COUNT, tokens.values.value.count)
    }

    @Test fun errorKeepsDraftAndRetryUsesIt() = runTest(dispatcher) {
        val vm = viewModel()
        vm.initialize(args)
        vm.selectCount(SELECTED_COUNT)
        tokens.failWrite = true
        vm.save()
        runCurrent()
        assertEquals(SaveState.ERROR, vm.state.value.save)
        assertEquals(SELECTED_COUNT, vm.state.value.count)
        assertTrue(vm.state.value.editable)
        tokens.failWrite = false
        vm.save()
        runCurrent()
        assertEquals(SELECTED_COUNT, tokens.values.value.count)
        assertEquals(SaveState.SAVED, vm.state.value.save)
    }

    @Test fun restoredInFlightWriteDoesNotRemainSavingOrRetryAutomatically() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        tokens.beforeWrite = { gate.await() }
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.initialize(args)
        vm.selectCount(SELECTED_COUNT)
        vm.save()
        runCurrent()
        val snapshot = copy(handle)
        store.clear()
        tokens.beforeWrite = {}
        val restored = viewModel(snapshot)
        restored.initialize(args)
        runCurrent()
        assertEquals(SaveState.IDLE, restored.state.value.save)
        assertEquals(SELECTED_COUNT, restored.state.value.count)
        assertEquals(FakeBoardRepository.BOARD_SIZE, tokens.values.value.count)
    }

    @Test fun completedWriteRemainsCompletedAfterStateRestore() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.initialize(args)
        vm.save()
        runCurrent()
        val restored = viewModel(copy(handle))
        restored.initialize(args)
        assertEquals(SaveState.SAVED, restored.state.value.save)
        assertFalse(restored.state.value.editable)
    }

    @Test fun invalidCountsAreIgnoredAndSaveBeforeInitializationIsIgnored() = runTest(dispatcher) {
        val vm = viewModel()
        vm.save()
        vm.selectCount(SELECTED_COUNT)
        runCurrent()
        assertNull(vm.state.value.count)
        vm.initialize(args)
        vm.selectCount(MIN_TOKEN_COUNT - COUNT_STEP)
        vm.selectCount(MAX_TOKEN_COUNT + COUNT_STEP)
        assertEquals(FakeBoardRepository.BOARD_SIZE, vm.state.value.count)
    }

    @Test fun reducerDoesNotMutatePreviousStateAndBoundsRestoredCount() {
        val original = SelectTokensNumberUiState(count = MAX_TOKEN_COUNT)
        val bounded = reduceTokensNumber(original, SelectTokensNumberAction.Initialize(MIN_TOKEN_COUNT, SELECTED_COUNT, MIN_TOKEN_COUNT))
        assertEquals(MAX_TOKEN_COUNT, original.count)
        assertEquals(SELECTED_COUNT, bounded.count)
    }

    private fun copy(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })

    private companion object {
        const val SELECTED_COUNT = 7
        const val COUNT_STEP = 1
    }
}
