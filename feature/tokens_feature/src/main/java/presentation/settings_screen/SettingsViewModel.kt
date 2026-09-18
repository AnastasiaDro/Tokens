package presentation.settings_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import domain.repository.TokenBoardRepository
import domain.repository.WinEffectsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import presentation.state.*

class SettingsViewModel(
    private val tokens: TokenBoardRepository,
    private val effects: WinEffectsRepository,
    private val reinforcement: ReinforcementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private val writes = Mutex()
    private var retryWrite: (suspend () -> Unit)? = null

    init { observe() }
    private fun dispatch(action: SettingsAction) { mutableState.update { reduceSettings(it, action) } }

    private fun observe(retryPendingWrite: Boolean = false) {
        observation?.cancel()
        dispatch(SettingsAction.Loading)
        observation = viewModelScope.launch {
            var shouldRetryWrite = retryPendingWrite
            try {
                observeSettings(tokens, effects, reinforcement).collect {
                    dispatch(SettingsAction.Loaded(it))
                    if (shouldRetryWrite) {
                        shouldRetryWrite = false
                        retryFailedWrite()
                    }
                }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { dispatch(SettingsAction.ReadFailed) }
        }
    }

    fun retry() {
        val current = state.value
        when {
            current.readFailure -> observe(current.writeFailure && retryWrite != null)
            current.writeFailure -> retryFailedWrite()
            else -> observe()
        }
    }

    private fun retryFailedWrite() {
        val action = retryWrite ?: return
        retryWrite = null
        save(action)
    }

    fun changeAnimation(enabled: Boolean) = save { effects.setAnimation(enabled) }
    fun changeSound(enabled: Boolean) = save { effects.setSound(enabled) }
    fun changeReinforcement(enabled: Boolean) = save { reinforcement.setEnabled(enabled) }

    private fun save(action: suspend () -> Unit) {
        if (state.value.loading || state.value.tokens == null || state.value.error == StorageFailure.READ) return
        viewModelScope.launch {
            writes.withLock {
                retryWrite = null
                dispatch(SettingsAction.WriteStarted)
                try {
                    action()
                    dispatch(SettingsAction.WriteSucceeded)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    retryWrite = action
                    dispatch(SettingsAction.WriteFailed)
                }
            }
        }
    }
}
