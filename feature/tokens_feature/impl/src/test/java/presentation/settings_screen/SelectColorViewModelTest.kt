package presentation.settings_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
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
import presentation.state.SaveState
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SelectColorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val tokens = FakeBoardRepository()

    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        SelectColorViewModel(tokens, handle).also { store.put(UUID.randomUUID().toString(), it) }

    @Test fun loadingBlocksEditsAndConfirmationButAllowsCancellation() = runTest(dispatcher) {
        val vm = viewModel()
        vm.selectColor(SELECTED_COLOR)
        vm.save()
        assertNull(vm.state.value.color)
        assertFalse(vm.state.value.editable)
        assertTrue(vm.state.value.cancellable)
        runCurrent()
        assertEquals(FakeBoardRepository.COLOR, vm.state.value.color)
        assertEquals(SaveState.IDLE, vm.state.value.save)
    }

    @Test fun repositoryEmissionsDoNotResetDraftOrWriteIt() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.selectColor(SELECTED_COLOR)
        tokens.setColor(EXTERNAL_COLOR)
        runCurrent()
        assertEquals(SELECTED_COLOR, vm.state.value.color)
        assertEquals(EXTERNAL_COLOR, tokens.values.value.color)
    }

    @Test fun restoredDraftWaitsForReadAndNeverWritesAutomatically() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        runCurrent()
        vm.selectColor(SELECTED_COLOR)
        val restored = viewModel(copy(handle))
        restored.save()
        assertFalse(restored.state.value.editable)
        assertEquals(SELECTED_COLOR, restored.state.value.color)
        runCurrent()
        assertTrue(restored.state.value.editable)
        assertEquals(SELECTED_COLOR, restored.state.value.color)
        assertEquals(FakeBoardRepository.COLOR, tokens.values.value.color)
    }

    @Test fun readFailureRetryPreservesDraftAndBlocksConfirmationUntilLoaded() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.selectColor(SELECTED_COLOR)
        tokens.failRead = true
        runCurrent()
        assertTrue(vm.state.value.readError)
        assertTrue(vm.state.value.cancellable)
        vm.save()
        tokens.failRead = false
        vm.retryRead()
        assertTrue(vm.state.value.loading)
        vm.save()
        runCurrent()
        assertEquals(SaveState.IDLE, vm.state.value.save)
        assertEquals(SELECTED_COLOR, vm.state.value.color)
        assertTrue(vm.state.value.editable)
        assertEquals(FakeBoardRepository.COLOR, tokens.values.value.color)
    }

    @Test fun duplicateConfirmationAndEditingAreBlockedDuringWrite() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val writes = mutableListOf<Unit>()
        tokens.beforeWrite = { writes += Unit; gate.await() }
        val vm = viewModel()
        runCurrent()
        vm.selectColor(SELECTED_COLOR)
        vm.save()
        vm.save()
        vm.selectColor(EXTERNAL_COLOR)
        runCurrent()
        assertEquals(listOf(Unit), writes)
        assertEquals(SELECTED_COLOR, vm.state.value.color)
        assertFalse(vm.state.value.cancellable)
        gate.complete(Unit)
        runCurrent()
        vm.save()
        runCurrent()
        assertEquals(listOf(Unit), writes)
        assertEquals(SELECTED_COLOR, tokens.values.value.color)
        assertEquals(SaveState.SAVED, vm.state.value.save)
    }

    @Test fun independentReadAndWriteErrorsKeepDraftForRetry() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        tokens.beforeWrite = { gate.await() }
        tokens.failWrite = true
        val vm = viewModel()
        runCurrent()
        vm.selectColor(SELECTED_COLOR)
        vm.save()
        runCurrent()
        tokens.failRead = true
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(SaveState.ERROR, vm.state.value.save)
        assertTrue(vm.state.value.readError)
        assertFalse(vm.state.value.editable)
        tokens.failRead = false
        tokens.failWrite = false
        vm.retryRead()
        runCurrent()
        assertEquals(SaveState.ERROR, vm.state.value.save)
        assertEquals(SELECTED_COLOR, vm.state.value.color)
        vm.save()
        runCurrent()
        assertEquals(SELECTED_COLOR, tokens.values.value.color)
        assertEquals(SaveState.SAVED, vm.state.value.save)
    }

    @Test fun inFlightWriteDoesNotRestartAfterProcessStateRestoration() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        tokens.beforeWrite = { gate.await() }
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        runCurrent()
        vm.selectColor(SELECTED_COLOR)
        vm.save()
        runCurrent()
        val snapshot = copy(handle)
        store.clear()
        tokens.beforeWrite = {}
        val restored = viewModel(snapshot)
        runCurrent()
        assertEquals(SaveState.IDLE, restored.state.value.save)
        assertEquals(SELECTED_COLOR, restored.state.value.color)
        assertEquals(FakeBoardRepository.COLOR, tokens.values.value.color)
    }

    @Test fun completedWriteRemainsCompletedAndUneditedColorIsExact() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        runCurrent()
        vm.save()
        runCurrent()
        val restored = viewModel(copy(handle))
        runCurrent()
        assertEquals(SaveState.SAVED, restored.state.value.save)
        assertFalse(restored.state.value.editable)
        assertEquals(FakeBoardRepository.COLOR, tokens.values.value.color)
    }

    @Test fun reducerIsImmutableAndIgnoresEditsDuringFailure() {
        val original = ColorUiState(color = SELECTED_COLOR, loading = false, readError = true)
        assertEquals(original, reduceColor(original, ColorAction.Select(EXTERNAL_COLOR)))
        val next = reduceColor(original, ColorAction.Loaded(EXTERNAL_COLOR))
        assertTrue(original.readError)
        assertFalse(next.readError)
        assertEquals(SELECTED_COLOR, next.color)
    }

    private fun copy(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
    private companion object {
        const val SELECTED_COLOR = -65536
        const val EXTERNAL_COLOR = -16711936
    }
}
