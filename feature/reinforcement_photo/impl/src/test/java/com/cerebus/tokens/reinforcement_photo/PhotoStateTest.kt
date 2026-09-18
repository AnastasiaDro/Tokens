package com.cerebus.tokens.reinforcement_photo

import androidx.lifecycle.SavedStateHandle
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
    private var store = ViewModelStore()
    private val repository = FakeRepository()
    private val files = FakeFiles()
    private var handle = SavedStateHandle()
    private lateinit var vm: ChangePhotoViewModel
    @Before fun before() {
        Dispatchers.setMain(dispatcher)
        create()
    }
    private fun create() {
        vm = ChangePhotoViewModel(repository, files, handle)
        store.put("photo", vm)
    }
    private fun restore() {
        val snapshot = handle.keys().associateWith { handle.get<Any?>(it) }
        store.clear()
        store = ViewModelStore()
        handle = SavedStateHandle(snapshot)
        create()
    }
    @After fun after() { store.clear(); Dispatchers.resetMain() }
    private fun select() {
        vm.chooseGallery()
        vm.consumeLaunch()
        vm.onGalleryResult(PHOTO_URI)
    }

    @Test fun cancelledSelectionIsNotSuccessAndKeepsPreviousPhoto() = runTest(dispatcher) {
        repository.values.value = ReinforcementSettings(photoUri = OLD_URI)
        runCurrent()
        vm.chooseGallery()
        vm.consumeLaunch()
        vm.onGalleryResult(null)
        assertFalse(vm.state.value.saved)
        assertEquals(OLD_URI, repository.values.value.photoUri)
        assertTrue(files.removed.isEmpty())
    }

    @Test fun savingAwaitsImportAndPersistenceAndSurvivesLateSubscriber() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        files.beforeImport = { gate.await() }
        select()
        runCurrent()
        assertTrue(vm.state.value.saving)
        assertFalse(vm.cancel())
        assertFalse(vm.state.value.saved)
        gate.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(IMPORTED_URI, repository.settings.first().photoUri)
        assertEquals(IMPORTED_URI, vm.state.value.photoUri)
    }

    @Test fun failedSaveRetriesPreparedCopyWithoutImportingAgain() = runTest(dispatcher) {
        runCurrent()
        repository.fail = true
        select()
        runCurrent()
        assertEquals(PhotoError.WRITE, vm.state.value.error)
        assertFalse(vm.state.value.saved)
        assertNull(repository.values.value.photoUri)
        repository.fail = false
        vm.retry()
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(IMPORTED_URI, repository.values.value.photoUri)
        assertEquals(SINGLE_OPERATION, files.imports)
    }

    @Test fun readRecoveryRetriesFailedPhotoSaveOnceAndKeepsObserving() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repository.beforeWrite = { gate.await() }
        select()
        runCurrent()
        repository.failRead = true
        runCurrent()
        assertTrue(vm.state.value.readFailure)
        assertTrue(vm.state.value.saving)
        repository.fail = true
        gate.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.writeFailure)
        assertEquals(PhotoError.READ, vm.state.value.error)
        repository.beforeWrite = {}
        repository.failRead = false
        repository.fail = false
        vm.retry()
        runCurrent()
        assertEquals(IMPORTED_URI, repository.values.value.photoUri)
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.readFailure)
        assertFalse(vm.state.value.writeFailure)
        assertEquals(RETRY_ATTEMPTS, repository.writeAttempts)
        assertEquals(SINGLE_OPERATION, repository.successfulWrites)
    }

    @Test fun captureUriSurvivesNewViewModelWithoutRelaunchAndDuplicateResultIsIgnored() = runTest(dispatcher) {
        runCurrent()
        vm.takePhoto()
        vm.takePhoto()
        runCurrent()
        assertEquals(ImageSource.CAMERA, vm.consumeLaunch())
        assertNull(vm.consumeLaunch())
        restore()
        runCurrent()
        assertEquals(CAPTURE_URI, vm.state.value.captureUri)
        assertEquals(ImageSource.CAMERA, vm.state.value.awaiting)
        assertNull(vm.consumeLaunch())
        vm.onCameraResult(true)
        vm.onCameraResult(true)
        runCurrent()
        assertEquals(CAPTURE_URI, repository.values.value.photoUri)
        assertEquals(SINGLE_OPERATION, files.captures)
        assertEquals(SINGLE_OPERATION, repository.successfulWrites)
    }

    @Test fun launchNotYetConsumedSurvivesRestoration() = runTest(dispatcher) {
        runCurrent()
        vm.chooseGallery()
        restore()
        runCurrent()
        assertEquals(ImageSource.GALLERY, vm.consumeLaunch())
        assertNull(vm.consumeLaunch())
    }

    @Test fun cameraCancellationDeletesOnlyPendingCapture() = runTest(dispatcher) {
        repository.values.value = ReinforcementSettings(photoUri = OLD_URI)
        runCurrent()
        vm.takePhoto()
        runCurrent()
        vm.consumeLaunch()
        vm.onCameraResult(false)
        assertEquals(listOf(CAPTURE_URI), files.removed)
        assertEquals(OLD_URI, repository.values.value.photoUri)
        assertFalse(vm.state.value.saved)
        assertTrue(vm.state.value.canSelect)
    }

    @Test fun missingCameraDoesNotBlockGalleryOrSaveAnEmptyFile() = runTest(dispatcher) {
        runCurrent()
        vm.takePhoto()
        runCurrent()
        vm.consumeLaunch()
        vm.sourceUnavailable()
        assertEquals(PhotoError.SOURCE, vm.state.value.error)
        assertEquals(listOf(CAPTURE_URI), files.removed)
        assertTrue(vm.state.value.canSelect)
        select()
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(IMPORTED_URI, repository.values.value.photoUri)
    }

    @Test fun failedSaveRestoresForExplicitRetryAndDoesNotAutoWrite() = runTest(dispatcher) {
        runCurrent()
        repository.fail = true
        select()
        runCurrent()
        restore()
        repository.fail = false
        runCurrent()
        assertEquals(SINGLE_OPERATION, repository.writeAttempts)
        assertEquals(PhotoError.WRITE, vm.state.value.error)
        vm.retry()
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(SINGLE_OPERATION, files.imports)
        assertEquals(RETRY_ATTEMPTS, repository.writeAttempts)
    }

    @Test fun earlyResultWaitsForInitialRead() = runTest(dispatcher) {
        runCurrent()
        vm.chooseGallery()
        vm.consumeLaunch()
        restore()
        vm.onGalleryResult(PHOTO_URI)
        assertTrue(vm.state.value.loading)
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(IMPORTED_URI, repository.values.value.photoUri)
    }

    @Test fun cancellingRestoredAlreadyCommittedCopyNeverDeletesCurrentImage() = runTest(dispatcher) {
        runCurrent()
        repository.fail = true
        select()
        runCurrent()
        repository.values.value = ReinforcementSettings(photoUri = IMPORTED_URI)
        restore()
        runCurrent()
        assertTrue(vm.cancel())
        assertTrue(files.removed.isEmpty())
        assertEquals(IMPORTED_URI, repository.values.value.photoUri)
    }

    @Test fun cancellationAfterFailedSaveDeletesOnlyPreparedCopy() = runTest(dispatcher) {
        repository.values.value = ReinforcementSettings(photoUri = OLD_URI)
        runCurrent()
        repository.fail = true
        select()
        runCurrent()
        assertTrue(vm.cancel())
        assertEquals(listOf(IMPORTED_URI), files.removed)
        assertEquals(OLD_URI, repository.values.value.photoUri)
    }

    @Test fun unreadableSelectionKeepsCurrentPhotoAndCanBeCancelled() = runTest(dispatcher) {
        repository.values.value = ReinforcementSettings(photoUri = OLD_URI)
        files.failImport = true
        runCurrent()
        select()
        runCurrent()
        assertEquals(PhotoError.WRITE, vm.state.value.error)
        assertEquals(OLD_URI, repository.values.value.photoUri)
        assertTrue(vm.cancel())
        assertTrue(files.removed.isEmpty())
    }

    private class FakeFiles : PhotoFiles {
        var captures = NO_OPERATIONS
        var imports = NO_OPERATIONS
        var failImport = false
        var beforeImport: suspend () -> Unit = {}
        val removed = mutableListOf<String>()
        override suspend fun createCapture(): String { captures++; return CAPTURE_URI }
        override suspend fun importGallery(uri: String): String {
            imports++
            beforeImport()
            if (failImport) throw IOException("Unavailable")
            return IMPORTED_URI
        }
        override suspend fun validateCapture(uri: String) = Unit
        override fun discard(uri: String) { removed += uri }
        override fun releaseSource(uri: String) = Unit
    }
    private class FakeRepository : ReinforcementRepository {
        val values = MutableStateFlow(ReinforcementSettings())
        private val readFailure = MutableStateFlow(false)
        var failRead: Boolean
            get() = readFailure.value
            set(value) { readFailure.value = value }
        var fail = false
        var beforeWrite: suspend () -> Unit = {}
        var writeAttempts = NO_OPERATIONS
        var successfulWrites = NO_OPERATIONS
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
    }
    private companion object {
        const val PHOTO_URI = "content://test/selected-photo"
        const val OLD_URI = "content://test/previous-photo"
        const val CAPTURE_URI = "content://own/camera.jpg"
        const val IMPORTED_URI = "content://own/imported.img"
        const val NO_OPERATIONS = 0
        const val SINGLE_OPERATION = 1
        const val RETRY_ATTEMPTS = 2
    }
}
