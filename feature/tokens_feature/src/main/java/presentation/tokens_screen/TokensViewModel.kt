package presentation.tokens_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import presentation.state.*

class TokensViewModel(
    private val tokens: TokenBoardRepository,
    private val effects: WinEffectsRepository,
    private val reinforcement: ReinforcementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TokensUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var timer: Job? = null
    private var foreground = false
    private var visit = INITIAL_GENERATION
    private var celebrationId = WinEffectsState.NO_CELEBRATION_ID
    private var celebrationRevision: Long? = null
    private var snapshot: SettingsSnapshot? = null
    private val writes = Mutex()
    private var retryWrite: (() -> Unit)? = null

    init { observe() }

    private fun dispatch(action: TokensAction) { mutableState.update { reduceTokens(it, action) } }

    private fun observe() {
        observation?.cancel()
        dispatch(TokensAction.Loading)
        observation = viewModelScope.launch {
            try {
                observeSettings(tokens, effects, reinforcement).collect {
                    if (celebrationRevision != null && celebrationRevision != it.board.revision) stopWinEffects()
                    snapshot = it
                    dispatch(TokensAction.Loaded(it))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                stopWinEffects()
                dispatch(TokensAction.Failed(StorageFailure.READ))
            }
        }
    }

    fun retry() {
        if (state.value.error == StorageFailure.WRITE) retryWrite?.invoke() else observe()
    }

    fun onStart() { foreground = true }
    fun onStop() { foreground = false; visit += GENERATION_STEP; stopWinEffects() }

    fun onTokenClicked(id: String) {
        if (state.value.loading || state.value.board == null || state.value.error == StorageFailure.READ) return
        val clickVisit = visit
        mutate({ onTokenClicked(id) }) {
            val result = tokens.toggle(id)
            if (result.changed && !result.board.completed) stopWinEffects()
            if (result.completedByUser && foreground && clickVisit == visit &&
                tokens.board.first().revision == result.board.revision) {
                val flags = snapshot?.effects ?: return@mutate
                timer?.cancel()
                celebrationRevision = result.board.revision
                celebrationId += GENERATION_STEP
                dispatch(TokensAction.Effects(WinEffectsState(flags.animation, flags.sound, celebrationId)))
                timer = viewModelScope.launch {
                    delay(WIN_EFFECTS_DURATION_MS)
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
                retryWrite = retry
                dispatch(TokensAction.Saving(true))
                try {
                    action()
                    retryWrite = null
                    dispatch(TokensAction.Saving(false))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { dispatch(TokensAction.Failed(StorageFailure.WRITE)) }
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
