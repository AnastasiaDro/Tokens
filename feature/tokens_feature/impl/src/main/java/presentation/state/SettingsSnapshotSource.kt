package presentation.state

import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import domain.repository.TokenBoardRepository
import domain.repository.WinEffectsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SettingsSnapshotState(
    val snapshot: SettingsSnapshot? = null,
    val loading: Boolean = true,
    val readFailure: Boolean = false,
) {
    val ready: Boolean get() = snapshot != null && !loading && !readFailure
}

/** DI owns this source and its dedicated scope, independently of either screen. */
class SettingsSnapshotSource(
    tokens: TokenBoardRepository,
    effects: WinEffectsRepository,
    reinforcement: ReinforcementRepository,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val mutableState = MutableStateFlow(SettingsSnapshotState())
    val state = mutableState.asStateFlow()
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        requests.trySend(Unit)
        scope.launch {
            // The next attempt starts only after the previous collection and its children end.
            for (request in requests) {
                try {
                    observeSettings(tokens, effects, reinforcement).collect { snapshot ->
                        mutableState.value = SettingsSnapshotState(snapshot, loading = false)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.update { it.copy(loading = false, readFailure = true) }
                }
            }
        }
    }

    fun retry() {
        if (!scope.isActive) return
        val current = state.value
        if (current.loading || !current.readFailure) return
        // Coalesce simultaneous retries from both screens without restarting healthy observation.
        if (mutableState.compareAndSet(current, current.copy(loading = true))) {
            requests.trySend(Unit)
        }
    }

    override fun close() {
        scope.cancel()
        requests.close()
    }
}
