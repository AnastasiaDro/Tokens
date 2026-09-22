package presentation.tokens_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import presentation.state.*
import kotlin.time.Duration.Companion.milliseconds

class TokensViewModel(
    private val tokens: TokenBoardRepository,
    private val source: SettingsSnapshotSource,
) : ViewModel() {
    private val mutableState = MutableStateFlow(reduceTokens(TokensUiState(), TokensAction.Observed(source.state.value)))
    val state = mutableState.asStateFlow()
    private var retryWriteAfterRead = false
    private var timer: Job? = null
    private var foreground = false
    private var visit = INITIAL_GENERATION
    private var celebrationId = WinEffectsState.NO_CELEBRATION_ID
    private var celebrationRevision: Long? = null
    private var snapshot: SettingsSnapshot? = source.state.value.snapshot
    private val writes = Mutex()
    private var retryWrite: (() -> Unit)? = null

    init { viewModelScope.launch { source.state.collect(::onSnapshot) } }

    private fun dispatch(action: TokensAction) { mutableState.update { reduceTokens(it, action) } }

    private fun onSnapshot(observed: SettingsSnapshotState) {
        if (observed.readFailure ||
            (celebrationRevision != null && celebrationRevision != observed.snapshot?.board?.revision)) {
            stopWinEffects()
        }
        snapshot = observed.snapshot
        dispatch(TokensAction.Observed(observed))
        if (observed.ready && retryWriteAfterRead) {
            retryWriteAfterRead = false
            retryFailedWrite()
        }
    }

    fun retry() {
        val current = state.value
        when {
            current.readFailure -> {
                retryWriteAfterRead = retryWriteAfterRead || (current.writeFailure && retryWrite != null)
                source.retry()
                onSnapshot(source.state.value)
            }
            current.writeFailure -> retryFailedWrite()
            else -> source.retry()
        }
    }

    private fun retryFailedWrite() {
        val action = retryWrite ?: return
        retryWrite = null
        action()
    }

    fun onStart() { foreground = true }
    fun onStop() { foreground = false; visit += GENERATION_STEP; stopWinEffects() }

    fun onTokenClicked(id: String) {
        if (state.value.loading || state.value.board == null || state.value.error == StorageFailure.READ) return
        val clickVisit = visit
        mutate({ onTokenClicked(id) }) {
            val result = tokens.toggle(id)
            if (result.changed && !result.board.completed) stopWinEffects()
            // A read needed only to guard an effect must not turn a committed toggle
            // into a retryable write failure (retrying would toggle it back).
            val currentRevision = if (result.completedByUser) {
                try { tokens.board.first().revision }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { return@mutate }
            } else null
            if (result.completedByUser && foreground && clickVisit == visit &&
                currentRevision == result.board.revision) {
                val flags = snapshot?.effects ?: return@mutate
                timer?.cancel()
                celebrationRevision = result.board.revision
                celebrationId += GENERATION_STEP
                dispatch(TokensAction.Effects(WinEffectsState(flags.animation, flags.sound, celebrationId)))
                timer = viewModelScope.launch {
                    delay(WIN_EFFECTS_DURATION_MS.milliseconds)
                    stopWinEffects()
                }
            }
        }
    }

    fun clearTokens() {
        if (state.value.loading || state.value.board == null || state.value.error == StorageFailure.READ) return
        visit += GENERATION_STEP
        stopWinEffects()
        mutate(::clearTokens) { tokens.clear() }
    }

    private fun mutate(retry: () -> Unit, action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                retryWrite = null
                dispatch(TokensAction.WriteStarted)
                try {
                    action()
                    dispatch(TokensAction.WriteSucceeded)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    retryWrite = retry
                    dispatch(TokensAction.WriteFailed)
                }
            }
        }
    }

    fun stopWinEffects() {
        timer?.cancel()
        timer = null
        celebrationRevision = null
        dispatch(TokensAction.Effects(WinEffectsState(false, false)))
    }

    private companion object {
        const val INITIAL_GENERATION = 0L
        const val GENERATION_STEP = 1L
    }
}
