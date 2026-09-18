package presentation.settings_screen

import presentation.state.SaveState

data class ColorUiState(
    val color: Int? = null,
    val loading: Boolean = true,
    val readError: Boolean = false,
    val save: SaveState = SaveState.IDLE,
) {
    val cancellable: Boolean get() = save != SaveState.SAVING && save != SaveState.SAVED
    val editable: Boolean get() = color != null && !loading && !readError && cancellable
}

sealed interface ColorAction {
    data object ReadStarted : ColorAction
    data class Loaded(val color: Int) : ColorAction
    data object ReadFailed : ColorAction
    data class Select(val color: Int) : ColorAction
    data object SaveStarted : ColorAction
    data object SaveSucceeded : ColorAction
    data object SaveFailed : ColorAction
}

fun reduceColor(state: ColorUiState, action: ColorAction): ColorUiState = when (action) {
    ColorAction.ReadStarted -> state.copy(loading = true, readError = false)
    is ColorAction.Loaded -> state.copy(color = state.color ?: action.color, loading = false, readError = false)
    ColorAction.ReadFailed -> state.copy(loading = false, readError = true)
    is ColorAction.Select -> if (state.editable) state.copy(color = action.color, save = SaveState.IDLE) else state
    ColorAction.SaveStarted -> state.copy(save = SaveState.SAVING)
    ColorAction.SaveSucceeded -> state.copy(save = SaveState.SAVED)
    ColorAction.SaveFailed -> state.copy(save = SaveState.ERROR)
}
