package presentation.tokens_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.repository.TokenBoardRepository
import domain.repository.MIN_TOKEN_COUNT
import domain.repository.MAX_TOKEN_COUNT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import presentation.state.SaveState
import presentation.SelectTokensNumberAlertData

class SelectTokensNumberViewModel(
    private val tokens: TokenBoardRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SelectTokensNumberUiState(
        count = savedState[DRAFT_COUNT],
        save = if (savedState.get<Boolean>(SAVE_COMPLETED) == true) SaveState.SAVED else SaveState.IDLE,
    ))
    val state = mutableState.asStateFlow()
    private var initialized = false

    fun initialize(data: SelectTokensNumberAlertData) {
        if (initialized) return
        val minimum = data.minTokensNum.coerceIn(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT)
        val maximum = data.maxTokensNum.coerceIn(minimum, MAX_TOKEN_COUNT)
        dispatch(SelectTokensNumberAction.Initialize(minimum, maximum, data.currentTokensNum))
        initialized = true
    }

    fun selectCount(count: Int) {
        if (initialized) dispatch(SelectTokensNumberAction.Select(count))
    }

    fun save() {
        val current = state.value
        if (!initialized || !current.editable) return
        val count = current.count ?: return
        dispatch(SelectTokensNumberAction.SaveStarted)
        viewModelScope.launch {
            try { tokens.resize(count); dispatch(SelectTokensNumberAction.SaveSucceeded) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { dispatch(SelectTokensNumberAction.SaveFailed) }
        }
    }

    private fun dispatch(action: SelectTokensNumberAction) {
        val next = reduceTokensNumber(mutableState.value, action)
        next.count?.let { savedState[DRAFT_COUNT] = it }
        if (next.save == SaveState.SAVED) savedState[SAVE_COMPLETED] = true
        mutableState.value = next
    }

    private companion object {
        const val DRAFT_COUNT = "tokens_count_draft"
        const val SAVE_COMPLETED = "tokens_count_saved"
    }
}
