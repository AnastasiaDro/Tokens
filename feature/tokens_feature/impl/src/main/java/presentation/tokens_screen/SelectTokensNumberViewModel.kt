package presentation.tokens_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.repository.TokenBoardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import presentation.state.SaveState

class SelectTokensNumberViewModel(private val tokens: TokenBoardRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SaveState.IDLE)
    val state = mutableState.asStateFlow()

    fun changeTokensNum(count: Int) {
        if (state.value == SaveState.SAVING || state.value == SaveState.SAVED) return
        mutableState.value = SaveState.SAVING
        viewModelScope.launch {
            try { tokens.resize(count); mutableState.value = SaveState.SAVED }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = SaveState.ERROR }
        }
    }
}
