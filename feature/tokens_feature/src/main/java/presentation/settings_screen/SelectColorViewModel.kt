package presentation.settings_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.repository.TokenBoardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import presentation.state.SaveState

data class ColorUiState(val color: Int? = null, val readError: Boolean = false, val save: SaveState = SaveState.IDLE)

class SelectColorViewModel(private val tokens: TokenBoardRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ColorUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { retryRead() }

    fun retryRead() {
        observation?.cancel()
        mutableState.update { it.copy(readError = false) }
        observation = viewModelScope.launch {
            try { tokens.board.collect { board -> mutableState.update { it.copy(color = board.color, readError = false) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(readError = true) } }
        }
    }

    fun save(color: Int) {
        if (state.value.color == null || state.value.readError || state.value.save == SaveState.SAVING || state.value.save == SaveState.SAVED) return
        mutableState.update { it.copy(save = SaveState.SAVING) }
        viewModelScope.launch {
            try { tokens.setColor(color); mutableState.update { it.copy(save = SaveState.SAVED) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(save = SaveState.ERROR) } }
        }
    }
}

