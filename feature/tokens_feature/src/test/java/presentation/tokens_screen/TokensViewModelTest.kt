package presentation.tokens_screen

import androidx.lifecycle.ViewModelStore
import domain.repository.WIN_EFFECTS_DURATION_MS
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import presentation.*
import presentation.state.StorageFailure

@OptIn(ExperimentalCoroutinesApi::class)
class TokensViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val tokens = FakeBoardRepository()
    private val effects = FakeEffectsRepository()
    private lateinit var vm: TokensViewModel
    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = TokensViewModel(tokens, effects, FakeReinforcementRepository())
        store.put("tokens", vm)
        vm.onStart()
    }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun win() { tokens.values.value.tokens.forEach { vm.onTokenClicked(it.id) } }

    @Test fun victorySurvivesAbsentSubscriberAndExpires() = runTest(dispatcher) {
        runCurrent()
        win()
        runCurrent()
        assertTrue(vm.state.value.effects.isSoundPlaying)
        assertTrue(vm.state.value.effects.isAnimationRunning)
        advanceTimeBy(WIN_EFFECTS_DURATION_MS)
        runCurrent()
        assertFalse(vm.state.value.effects.isSoundPlaying)
    }

    @Test fun independentSoundAndAnimationSettings() = runTest(dispatcher) {
        runCurrent()
        for (animation in listOf(false, true)) for (sound in listOf(false, true)) {
            vm.clearTokens()
            effects.setAnimation(animation)
            effects.setSound(sound)
            runCurrent()
            win()
            runCurrent()
            assertEquals(animation, vm.state.value.effects.isAnimationRunning)
            assertEquals(sound, vm.state.value.effects.isSoundPlaying)
        }
    }

    @Test fun newVictoryCancelsOldTimerAndGetsFullDuration() = runTest(dispatcher) {
        runCurrent()
        win()
        runCurrent()
        val first = vm.state.value.effects.celebrationId
        advanceTimeBy(RESTART_DELAY_MS)
        val id = tokens.values.value.tokens.last().id
        vm.onTokenClicked(id)
        runCurrent()
        assertFalse(vm.state.value.effects.isSoundPlaying)
        vm.onTokenClicked(id)
        runCurrent()
        assertTrue(vm.state.value.effects.celebrationId > first)
        advanceTimeBy(WIN_EFFECTS_DURATION_MS - RESTART_DELAY_MS)
        runCurrent()
        assertTrue(vm.state.value.effects.isSoundPlaying)
        advanceTimeBy(RESTART_DELAY_MS)
        runCurrent()
        assertFalse(vm.state.value.effects.isSoundPlaying)
    }

    @Test fun leavingScreenPreventsPendingSaveFromStartingEffects() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        tokens.beforeWrite = { gate.await() }
        win()
        runCurrent()
        vm.onStop()
        gate.complete(Unit)
        runCurrent()
        vm.onStart()
        runCurrent()
        assertTrue(tokens.values.value.completed)
        assertFalse(vm.state.value.effects.isSoundPlaying)
    }

    @Test fun restoringFullBoardAndResizingNeverStartsVictory() = runTest(dispatcher) {
        tokens.values.value.tokens.forEach { tokens.setChecked(it.id, true) }
        runCurrent()
        assertFalse(vm.state.value.effects.isSoundPlaying)
        tokens.resize(SINGLE_TOKEN)
        runCurrent()
        assertTrue(tokens.values.value.completed)
        assertFalse(vm.state.value.effects.isAnimationRunning)
    }

    @Test fun rapidClicksToggleLatestValueAndIgnoreStaleId() = runTest(dispatcher) {
        runCurrent()
        val id = tokens.values.value.tokens.first().id
        vm.onTokenClicked(id)
        vm.onTokenClicked(id)
        vm.onTokenClicked("removed")
        runCurrent()
        assertTrue(tokens.values.value.tokens.none { it.isChecked })
        assertFalse(vm.state.value.effects.isSoundPlaying)
    }

    @Test fun failedWriteNeverWinsAndCanRetry() = runTest(dispatcher) {
        tokens.resize(SINGLE_TOKEN)
        runCurrent()
        tokens.failWrite = true
        vm.onTokenClicked(tokens.values.value.tokens.single().id)
        runCurrent()
        assertEquals(StorageFailure.WRITE, vm.state.value.error)
        assertFalse(vm.state.value.effects.isSoundPlaying)
        tokens.failWrite = false
        vm.retry()
        runCurrent()
        assertTrue(vm.state.value.effects.isSoundPlaying)
    }

    @Test fun clearAndExternalResizeStopEffects() = runTest(dispatcher) {
        runCurrent()
        win()
        runCurrent()
        tokens.resize(SINGLE_TOKEN)
        runCurrent()
        assertFalse(vm.state.value.effects.isSoundPlaying)
        vm.clearTokens()
        runCurrent()
        assertFalse(tokens.values.value.completed)
    }

    private companion object {
        const val RESTART_DELAY_MS = 1000L
        const val SINGLE_TOKEN = 1
    }
}
