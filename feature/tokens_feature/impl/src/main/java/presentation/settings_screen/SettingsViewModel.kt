package presentation.settings_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import domain.repository.WinEffectsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import presentation.state.*

class SettingsViewModel(
    private val source: SettingsSnapshotSource,
    private val effects: WinEffectsRepository,
    private val reinforcement: ReinforcementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(reduceSettings(SettingsUiState(), SettingsAction.Observed(source.state.value)))
    val state = mutableState.asStateFlow()
    private var retryWriteAfterRead = false
    private val writes = Mutex()
    private var retryWrite: (suspend () -> Unit)? = null

    init { viewModelScope.launch { source.state.collect(::onSnapshot) } }
    private fun dispatch(action: SettingsAction) { mutableState.update { reduceSettings(it, action) } }

    private fun onSnapshot(snapshot: SettingsSnapshotState) {
        dispatch(SettingsAction.Observed(snapshot))
        if (snapshot.ready && retryWriteAfterRead) {
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
