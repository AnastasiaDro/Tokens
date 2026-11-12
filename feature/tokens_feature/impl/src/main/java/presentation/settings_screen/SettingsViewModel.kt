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
import presentation.settings_screen.SettingsReducer.loadingStarted
import presentation.settings_screen.SettingsReducer.readFailed
import presentation.settings_screen.SettingsReducer.snapshotLoaded
import presentation.settings_screen.SettingsReducer.snapshotObserved
import presentation.settings_screen.SettingsReducer.writeFailed
import presentation.settings_screen.SettingsReducer.writeStarted
import presentation.settings_screen.SettingsReducer.writeSucceeded

class SettingsViewModel(
    private val source: SettingsSnapshotSource,
    private val effects: WinEffectsRepository,
    private val reinforcement: ReinforcementRepository,
    internal val navigator: SettingsNavigator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState().snapshotObserved(source.state.value))
    val state = mutableState.asStateFlow()
    private var retryWriteAfterRead = false
    private val writes = Mutex()
    private var retryWrite: (suspend () -> Unit)? = null

    init { viewModelScope.launch { source.state.collect(::onSnapshot) } }
    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.SelectCountClicked -> if (state.value.editable) {
                state.value.tokens?.let { navigator.selectCount(it.count) }
            }
            SettingsAction.SelectColorClicked -> if (state.value.editable) navigator.selectColor()
            is SettingsAction.AnimationChanged -> if (state.value.editable) save { effects.setAnimation(action.enabled) }
            is SettingsAction.SoundChanged -> if (state.value.editable) save { effects.setSound(action.enabled) }
            is SettingsAction.ReinforcementChanged -> if (state.value.editable) save { reinforcement.setEnabled(action.enabled) }
            SettingsAction.RetryClicked -> if (state.value.error != null && !state.value.loading) retry()
            SettingsAction.YoutubeClicked -> navigator.youtube()
            SettingsAction.OtherAppsClicked -> navigator.otherApps()
            is SettingsAction.Observed -> updateState { snapshotObserved(action.source) }
            SettingsAction.Loading -> updateState { loadingStarted() }
            is SettingsAction.Loaded -> updateState { snapshotLoaded(action.snapshot) }
            SettingsAction.ReadFailed -> updateState { readFailed() }
            SettingsAction.WriteStarted -> updateState { writeStarted() }
            SettingsAction.WriteSucceeded -> updateState { writeSucceeded() }
            SettingsAction.WriteFailed -> updateState { writeFailed() }
        }
    }

    private fun updateState(reduce: SettingsUiState.() -> SettingsUiState) {
        mutableState.update { it.reduce() }
    }

    private fun onSnapshot(snapshot: SettingsSnapshotState) {
        onAction(SettingsAction.Observed(snapshot))
        if (snapshot.ready && retryWriteAfterRead) {
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
        save(action)
    }

    private fun save(action: suspend () -> Unit) {
        if (state.value.loading || state.value.tokens == null || state.value.error == StorageFailure.READ) return
        viewModelScope.launch {
            writes.withLock {
                retryWrite = null
                onAction(SettingsAction.WriteStarted)
                try {
                    action()
                    onAction(SettingsAction.WriteSucceeded)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    retryWrite = action
                    onAction(SettingsAction.WriteFailed)
                }
            }
        }
    }
}
