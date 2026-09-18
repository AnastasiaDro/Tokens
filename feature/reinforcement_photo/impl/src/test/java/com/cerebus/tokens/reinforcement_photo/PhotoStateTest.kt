package com.cerebus.tokens.reinforcement_photo

import androidx.lifecycle.ViewModelStore
import com.cerebus.tokens.data.reinforcement.*
import com.cerebus.tokens.reinforcement_photo.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    private lateinit var vm: ChangePhotoViewModel
    @Before fun before() {
        Dispatchers.setMain(dispatcher)
        vm = ChangePhotoViewModel(repository)
        store.put("photo", vm)
    }
    @After fun after() { store.clear(); Dispatchers.resetMain() }

    @Test fun cancelledSelectionIsNotSuccess() = runTest(dispatcher) {
        runCurrent()
        vm.saveSelection(null)
        runCurrent()
        assertFalse(vm.state.value.saved)
        assertNull(repository.values.value.photoUri)
    }

    @Test fun savingAwaitsAccessAndPersistenceAndSurvivesLateSubscriber() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        vm.saveSelection(PHOTO_URI) { gate.await() }
        runCurrent()
        assertTrue(vm.state.value.saving)
        assertFalse(vm.state.value.saved)
        gate.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(PHOTO_URI, vm.state.value.photoUri)
        assertEquals(PHOTO_URI, repository.settings.first().photoUri)
    }

    @Test fun failedSaveKeepsDialogOpenAndCanRetryWithoutSelectingAgain() = runTest(dispatcher) {
        runCurrent()
        repository.fail = true
        vm.saveSelection(PHOTO_URI)
        runCurrent()
        assertEquals(PhotoError.WRITE, vm.state.value.error)
        assertFalse(vm.state.value.saved)
        assertNull(repository.values.value.photoUri)
        repository.fail = false
        vm.retry()
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(PHOTO_URI, repository.values.value.photoUri)
    }

    @Test fun readRecoveryRetriesFailedPhotoSaveOnceAndKeepsObserving() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repository.beforeWrite = { gate.await() }
        vm.saveSelection(PHOTO_URI)
        runCurrent()

        repository.failRead = true
        runCurrent()
        assertTrue(vm.state.value.readFailure)
        assertTrue(vm.state.value.saving)

        repository.fail = true
        gate.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.readFailure)
        assertTrue(vm.state.value.writeFailure)
        assertEquals(PhotoError.READ, vm.state.value.error)

        repository.beforeWrite = {}
        repository.failRead = false
        repository.fail = false
        vm.retry()
        runCurrent()

        assertEquals(PHOTO_URI, repository.values.value.photoUri)
        assertEquals(PHOTO_URI, vm.state.value.photoUri)
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.readFailure)
        assertFalse(vm.state.value.writeFailure)
        assertEquals(EXPECTED_RETRY_ATTEMPTS, repository.writeAttempts)
        assertEquals(EXPECTED_SUCCESSFUL_WRITES, repository.successfulWrites)
    }

    private class FakeRepository : ReinforcementRepository {
        val values = MutableStateFlow(ReinforcementSettings())
        private val readFailure = MutableStateFlow(false)
        var failRead: Boolean
            get() = readFailure.value
            set(value) { readFailure.value = value }
        var fail = false
        var beforeWrite: suspend () -> Unit = {}
        var writeAttempts = NO_WRITES
        var successfulWrites = NO_WRITES
        override val settings = combine(values, readFailure) { settings, failed ->
            if (failed) throw IOException("Read failed")
            settings
        }
        override suspend fun setEnabled(enabled: Boolean) { values.update { it.copy(enabled = enabled) } }
        override suspend fun setPhotoUri(uri: String) {
            writeAttempts++
            beforeWrite()
            if (fail) throw IOException("Write failed")
            values.update { it.copy(photoUri = uri) }
            successfulWrites++
        }

        private companion object { const val NO_WRITES = 0 }
    }

    private companion object {
        const val PHOTO_URI = "content://test/selected-photo"
        const val EXPECTED_RETRY_ATTEMPTS = 2
        const val EXPECTED_SUCCESSFUL_WRITES = 1
    }
}
