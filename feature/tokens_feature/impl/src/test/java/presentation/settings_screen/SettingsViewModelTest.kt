package presentation.settings_screen

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import presentation.FakeBoardRepository
import presentation.FakeEffectsRepository
import presentation.FakeReinforcementRepository
import presentation.state.SettingsSnapshotSource

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val tokens = FakeBoardRepository()
    private val effects = FakeEffectsRepository()
    private val reinforcement = FakeReinforcementRepository()
    private val navigator = SettingsNavigator()
    private val destinations = mutableListOf<SettingsNavigator.Destination>()
    private lateinit var binding: AutoCloseable
    private lateinit var source: SettingsSnapshotSource
    private lateinit var vm: SettingsViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        source = SettingsSnapshotSource(tokens, effects, reinforcement, CoroutineScope(SupervisorJob() + dispatcher))
        binding = navigator.bind { destinations += it }
        vm = SettingsViewModel(source, effects, reinforcement, navigator)
        store.put("settings", vm)
    }

    @After fun tearDown() { store.clear(); source.close(); binding.close(); Dispatchers.resetMain() }

    @Test fun navigationUsesLatestCountAndDoesNotWriteOrReplayOnObservation() = runTest(dispatcher) {
        runCurrent()
        tokens.resize(CHANGED_COUNT)
        runCurrent()
        vm.onAction(SettingsAction.SelectCountClicked)
        vm.onAction(SettingsAction.SelectColorClicked)
        vm.onAction(SettingsAction.YoutubeClicked)
        vm.onAction(SettingsAction.OtherAppsClicked)
        val expected = listOf(SettingsNavigator.Destination.SelectCount(CHANGED_COUNT),
            SettingsNavigator.Destination.SelectColor, SettingsNavigator.Destination.Youtube,
            SettingsNavigator.Destination.OtherApps)
        assertEquals(expected, destinations)
        vm.state.first()
        vm.state.first()
        runCurrent()
        assertEquals(expected, destinations)
        assertEquals(NO_WRITES, effects.writeAttempts)
        assertFalse(reinforcement.settings.value.enabled)
    }

    @Test fun loadingAndReadFailureRejectEditsButKeepExternalLinksAvailable() = runTest(dispatcher) {
        editActions.forEach(vm::onAction)
        assertTrue(destinations.isEmpty())
        vm.onAction(SettingsAction.YoutubeClicked)
        runCurrent()
        assertEquals(NO_WRITES, effects.writeAttempts)
        assertFalse(reinforcement.settings.value.enabled)

        effects.failRead = true
        runCurrent()
        assertNotNull(vm.state.value.tokens)
        assertTrue(vm.state.value.readFailure)
        editActions.forEach(vm::onAction)
        vm.onAction(SettingsAction.OtherAppsClicked)
        runCurrent()
        assertEquals(listOf(SettingsNavigator.Destination.Youtube, SettingsNavigator.Destination.OtherApps), destinations)
        assertEquals(NO_WRITES, effects.writeAttempts)
        assertFalse(reinforcement.settings.value.enabled)
    }

    @Test fun savingRejectsEditsAndNavigationWithoutDuplicatingPendingWrite() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        effects.beforeWrite = { gate.await() }
        vm.onAction(SettingsAction.SoundChanged(false))
        runCurrent()
        assertTrue(vm.state.value.saving)
        assertTrue(vm.state.value.effects!!.sound)
        editActions.forEach(vm::onAction)
        vm.onAction(SettingsAction.RetryClicked)
        runCurrent()
        assertTrue(destinations.isEmpty())
        assertEquals(SINGLE_WRITE, effects.writeAttempts)
        assertFalse(reinforcement.settings.value.enabled)
        gate.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.saving)
        assertFalse(vm.state.value.effects!!.sound)
        assertTrue(vm.state.value.effects!!.animation)
        assertEquals(SINGLE_WRITE, effects.successfulWrites)
    }

    @Test fun switchesWriteThroughTheirRepositoriesWithoutUiSubscriberOrReplay() = runTest(dispatcher) {
        runCurrent()
        vm.onAction(SettingsAction.AnimationChanged(false))
        runCurrent()
        vm.onAction(SettingsAction.SoundChanged(false))
        runCurrent()
        vm.onAction(SettingsAction.ReinforcementChanged(true))
        runCurrent()
        assertFalse(vm.state.value.effects!!.animation)
        assertFalse(vm.state.value.effects!!.sound)
        assertTrue(vm.state.value.reinforcement!!.enabled)
        assertEquals(EFFECTS_WRITES, effects.successfulWrites)
        vm.state.first()
        vm.state.first()
        runCurrent()
        assertEquals(EFFECTS_WRITES, effects.writeAttempts)
        assertTrue(destinations.isEmpty())
    }

    @Test fun writeFailureKeepsSavedStateAndAllowsNavigationAndExplicitRetry() = runTest(dispatcher) {
        runCurrent()
        effects.failWrite = true
        vm.onAction(SettingsAction.AnimationChanged(false))
        runCurrent()
        assertTrue(vm.state.value.writeFailure)
        assertTrue(vm.state.value.effects!!.animation)
        vm.onAction(SettingsAction.SelectColorClicked)
        assertEquals(listOf(SettingsNavigator.Destination.SelectColor), destinations)
        effects.failWrite = false
        vm.onAction(SettingsAction.RetryClicked)
        runCurrent()
        assertFalse(vm.state.value.effects!!.animation)
        assertFalse(vm.state.value.writeFailure)
        assertEquals(RETRY_ATTEMPTS, effects.writeAttempts)
        assertEquals(SINGLE_WRITE, effects.successfulWrites)
    }

    private companion object {
        const val CHANGED_COUNT = 7
        const val NO_WRITES = 0
        const val SINGLE_WRITE = 1
        const val EFFECTS_WRITES = 2
        const val RETRY_ATTEMPTS = 2
        val editActions = listOf(SettingsAction.SelectCountClicked, SettingsAction.SelectColorClicked,
            SettingsAction.AnimationChanged(false), SettingsAction.SoundChanged(false),
            SettingsAction.ReinforcementChanged(true))
    }
}
