package presentation

import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import domain.repository.TokenBoardRepository
import domain.repository.WinEffectsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import presentation.state.SettingsSnapshotSource

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSnapshotSourceTest {
    private val board = FakeBoardRepository()
    private val effects = FakeEffectsRepository()
    private val reinforcement = FakeReinforcementRepository()
    private val boardReads = CountedFlow(board.board)
    private val effectReads = CountedFlow(effects.settings)
    private val photoReads = CountedFlow(reinforcement.settings)
    private val boardRepository = object : TokenBoardRepository by board { override val board = boardReads.flow }
    private val effectsRepository = object : WinEffectsRepository by effects { override val settings = effectReads.flow }
    private val photoRepository = object : ReinforcementRepository by reinforcement { override val settings = photoReads.flow }

    @Test fun waitsForAllSourcesAndSharesObservationEvenWithoutUiSubscribers() = runTest {
        val gate = CompletableDeferred<Unit>()
        val delayedEffects = object : WinEffectsRepository by effectsRepository {
            override val settings = effectsRepository.settings.onStart { gate.await() }
        }
        val source = SettingsSnapshotSource(boardRepository, delayedEffects, photoRepository, backgroundScope)
        assertNull(source.state.value.snapshot)
        runCurrent()
        assertNull(source.state.value.snapshot)
        assertTrue(source.state.value.loading)
        gate.complete(Unit)
        runCurrent()
        assertTrue(source.state.value.ready)

        val first = backgroundScope.launch { source.state.collect() }
        val second = backgroundScope.launch { source.state.collect() }
        runCurrent()
        first.cancel()
        second.cancel()
        effects.setSound(false)
        reinforcement.setEnabled(true)
        board.resize(NEW_COUNT)
        runCurrent()
        val snapshot = source.state.value.snapshot!!
        assertFalse(snapshot.effects.sound)
        assertTrue(snapshot.reinforcement.enabled)
        assertEquals(NEW_COUNT, snapshot.board.count)
        source.retry() // Healthy observation must not restart.
        runCurrent()
        listOf(boardReads, effectReads, photoReads).forEach {
            assertEquals(SINGLE_SUBSCRIPTION, it.starts)
            assertEquals(SINGLE_SUBSCRIPTION, it.active)
        }
        source.close()
        runCurrent()
        listOf(boardReads, effectReads, photoReads).forEach { assertEquals(NO_SUBSCRIPTIONS, it.active) }
    }

    @Test fun failureRetainsSnapshotAndConcurrentRetriesStartOnlyOneNewAttempt() = runTest {
        val source = SettingsSnapshotSource(boardRepository, effectsRepository, photoRepository, backgroundScope)
        runCurrent()
        val saved = source.state.value.snapshot
        effects.failRead = true
        runCurrent()
        assertTrue(source.state.value.readFailure)
        assertFalse(source.state.value.loading)
        assertEquals(saved, source.state.value.snapshot)
        listOf(boardReads, effectReads, photoReads).forEach { assertEquals(NO_SUBSCRIPTIONS, it.active) }

        effects.failRead = false
        repeat(RETRY_CALLS) { launch { source.retry() } }
        runCurrent()
        assertTrue(source.state.value.ready)
        listOf(boardReads, effectReads, photoReads).forEach {
            assertEquals(ATTEMPTS_AFTER_RETRY, it.starts)
            assertEquals(SINGLE_SUBSCRIPTION, it.maximumActive)
        }
        effects.setAnimation(false)
        runCurrent()
        assertFalse(source.state.value.snapshot!!.effects.animation)
    }

    @Test fun initialFailureCanBeRetriedRepeatedlyAndClosingDoesNotReportReadFailure() = runTest {
        effects.failRead = true
        val source = SettingsSnapshotSource(boardRepository, effectsRepository, photoRepository, backgroundScope)
        runCurrent()
        assertNull(source.state.value.snapshot)
        assertTrue(source.state.value.readFailure)
        source.retry()
        assertTrue(source.state.value.loading)
        assertTrue(source.state.value.readFailure)
        runCurrent()
        assertFalse(source.state.value.loading)
        assertTrue(source.state.value.readFailure)
        effects.failRead = false
        source.retry()
        runCurrent()
        assertTrue(source.state.value.ready)
        source.close()
        runCurrent()
        assertFalse(source.state.value.readFailure)
    }

    private class CountedFlow<T>(upstream: Flow<T>) {
        var starts = NO_SUBSCRIPTIONS
        var active = NO_SUBSCRIPTIONS
        var maximumActive = NO_SUBSCRIPTIONS
        val flow = flow {
            starts++
            active++
            maximumActive = maxOf(maximumActive, active)
            try { emitAll(upstream) } finally { active-- }
        }
    }

    private companion object {
        const val NO_SUBSCRIPTIONS = 0
        const val SINGLE_SUBSCRIPTION = 1
        const val ATTEMPTS_AFTER_RETRY = 2
        const val RETRY_CALLS = 10
        const val NEW_COUNT = 3
    }
}
