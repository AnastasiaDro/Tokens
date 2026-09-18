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

    private fun observe() {
        observation?.cancel()
        dispatch(SettingsAction.Loading)
        observation = viewModelScope.launch {
            try { observeSettings(tokens, effects, reinforcement).collect { dispatch(SettingsAction.Loaded(it)) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { dispatch(SettingsAction.Failed(StorageFailure.READ)) }
        }
    }

    fun retry() {
        if (state.value.error == StorageFailure.WRITE) retryWrite?.let { save(it) } else observe()
    }

    fun changeAnimation(enabled: Boolean) = save { effects.setAnimation(enabled) }
    fun changeSound(enabled: Boolean) = save { effects.setSound(enabled) }
    fun changeReinforcement(enabled: Boolean) = save { reinforcement.setEnabled(enabled) }

    private fun save(action: suspend () -> Unit) {
        if (state.value.loading || state.value.tokens == null || state.value.error == StorageFailure.READ) return
        viewModelScope.launch {
            writes.withLock {
                retryWrite = action
                dispatch(SettingsAction.Saving(true))
                try {
                    action()
                    retryWrite = null
                    dispatch(SettingsAction.Saving(false))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { dispatch(SettingsAction.Failed(StorageFailure.WRITE)) }
            }
        }
    }
}
