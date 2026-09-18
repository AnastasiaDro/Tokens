package presentation.settings_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.repository.TokenBoardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import presentation.state.SaveState

class SelectColorViewModel(
    private val tokens: TokenBoardRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ColorUiState(
        color = savedState[DRAFT_COLOR],
        save = if (savedState.get<Boolean>(SAVE_COMPLETED) == true) SaveState.SAVED else SaveState.IDLE,
    ))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { retryRead() }

    fun retryRead() {
        observation?.cancel()
        dispatch(ColorAction.ReadStarted)
        observation = viewModelScope.launch {
            try { tokens.board.collect { dispatch(ColorAction.Loaded(it.color)) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { dispatch(ColorAction.ReadFailed) }
        }
    }

    fun selectColor(color: Int) { dispatch(ColorAction.Select(color)) }

    fun save() {
        val current = state.value
        if (!current.editable) return
        val color = current.color ?: return
        dispatch(ColorAction.SaveStarted)
        viewModelScope.launch {
            try { tokens.setColor(color); dispatch(ColorAction.SaveSucceeded) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { dispatch(ColorAction.SaveFailed) }
        }
    }

    private fun dispatch(action: ColorAction) {
        val next = reduceColor(mutableState.value, action)
        next.color?.let { savedState[DRAFT_COLOR] = it }
        if (next.save == SaveState.SAVED) savedState[SAVE_COMPLETED] = true
        mutableState.value = next
    }

    private companion object {
        const val DRAFT_COLOR = "token_color_draft"
        const val SAVE_COMPLETED = "token_color_saved"
    }
}
