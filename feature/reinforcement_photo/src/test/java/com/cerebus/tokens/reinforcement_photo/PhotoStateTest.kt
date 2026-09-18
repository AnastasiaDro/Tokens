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
        assertNull(repository.settings.value.photoUri)
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
        assertNull(repository.settings.value.photoUri)
        repository.fail = false
        vm.retry()
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(PHOTO_URI, repository.settings.value.photoUri)
    }

    private class FakeRepository : ReinforcementRepository {
        override val settings = MutableStateFlow(ReinforcementSettings())
        var fail = false
        override suspend fun setEnabled(enabled: Boolean) { settings.update { it.copy(enabled = enabled) } }
        override suspend fun setPhotoUri(uri: String) {
            if (fail) throw IOException("Write failed")
            settings.update { it.copy(photoUri = uri) }
        }
    }

    private companion object { const val PHOTO_URI = "content://test/selected-photo" }
}

