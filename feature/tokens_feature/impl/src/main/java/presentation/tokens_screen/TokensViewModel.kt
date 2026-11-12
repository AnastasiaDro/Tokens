package presentation.tokens_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import presentation.state.*
import presentation.tokens_screen.TokensReducer.effectsChanged
import presentation.tokens_screen.TokensReducer.loadingStarted
import presentation.tokens_screen.TokensReducer.readFailed
import presentation.tokens_screen.TokensReducer.snapshotLoaded
import presentation.tokens_screen.TokensReducer.snapshotObserved
import presentation.tokens_screen.TokensReducer.writeFailed
import presentation.tokens_screen.TokensReducer.writeStarted
import presentation.tokens_screen.TokensReducer.writeSucceeded
import kotlin.time.Duration.Companion.milliseconds

class TokensViewModel(
    private val tokens: TokenBoardRepository,
    private val source: SettingsSnapshotSource,
    internal val navigator: TokensNavigator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TokensUiState().snapshotObserved(source.state.value))
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

    fun onAction(action: TokensAction) {
        when (action) {
            is TokensAction.TokenClicked -> if (canUseBoard() && state.value.board?.tokens?.any { it.id == action.id } == true) {
                toggleToken(action.id)
            }
            TokensAction.ClearClicked -> if (canUseBoard()) clearTokens()
            TokensAction.RetryClicked -> if (state.value.error != null && !state.value.saving) retry()
            TokensAction.SelectCountClicked -> if (canUseBoard()) state.value.board?.let { navigator.selectCount(it.count) }
            TokensAction.SettingsClicked -> { onStop(); navigator.settings() }
            TokensAction.PhotoClicked -> if (state.value.reinforcement?.enabled == true) navigator.photo()
            is TokensAction.Observed -> updateState { snapshotObserved(action.source) }
            TokensAction.Loading -> updateState { loadingStarted() }
            is TokensAction.Loaded -> updateState { snapshotLoaded(action.snapshot) }
            TokensAction.ReadFailed -> updateState { readFailed() }
            TokensAction.WriteStarted -> updateState { writeStarted() }
            TokensAction.WriteSucceeded -> updateState { writeSucceeded() }
            TokensAction.WriteFailed -> updateState { writeFailed() }
            is TokensAction.Effects -> updateState { effectsChanged(action.effects) }
        }
    }

    private fun updateState(reduce: TokensUiState.() -> TokensUiState) {
        mutableState.update { it.reduce() }
    }

    private fun canUseBoard(): Boolean = state.value.let { !it.loading && it.board != null && !it.readFailure }

    private fun onSnapshot(observed: SettingsSnapshotState) {
        if (observed.readFailure ||
            (celebrationRevision != null && celebrationRevision != observed.snapshot?.board?.revision)) {
            stopWinEffects()
        }
        snapshot = observed.snapshot
        onAction(TokensAction.Observed(observed))
        if (observed.ready && retryWriteAfterRead) {
            retryWriteAfterRead = false
            retryFailedWrite()
        }
    }

    private fun retry() {
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

    private fun toggleToken(id: String) {
        val clickVisit = visit
        mutate({ onAction(TokensAction.TokenClicked(id)) }) {
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
                onAction(TokensAction.Effects(WinEffectsState(flags.animation, flags.sound, celebrationId)))
                timer = viewModelScope.launch {
                    delay(WIN_EFFECTS_DURATION_MS.milliseconds)
                    stopWinEffects()
                }
            }
        }
    }

    private fun clearTokens() {
        visit += GENERATION_STEP
        stopWinEffects()
        mutate({ onAction(TokensAction.ClearClicked) }) { tokens.clear() }
    }

    private fun mutate(retry: () -> Unit, action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                retryWrite = null
                onAction(TokensAction.WriteStarted)
                try {
                    action()
                    onAction(TokensAction.WriteSucceeded)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    retryWrite = retry
                    onAction(TokensAction.WriteFailed)
                }
            }
        }
    }

    fun stopWinEffects() {
        timer?.cancel()
        timer = null
        celebrationRevision = null
        onAction(TokensAction.Effects(WinEffectsState(false, false)))
    }

    private companion object {
        const val INITIAL_GENERATION = 0L
        const val GENERATION_STEP = 1L
    }
}
