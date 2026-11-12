package presentation.tokens_screen

import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import presentation.state.SaveState

data class SelectTokensNumberUiState(
    val count: Int? = null,
    val minimum: Int = MIN_TOKEN_COUNT,
    val maximum: Int = MAX_TOKEN_COUNT,
    val save: SaveState = SaveState.IDLE,
) {
    val editable: Boolean get() = count != null && save != SaveState.SAVING && save != SaveState.SAVED
}

sealed interface SelectTokensNumberAction {
    data class Initialize(val minimum: Int, val maximum: Int, val count: Int) : SelectTokensNumberAction
    data class Select(val count: Int) : SelectTokensNumberAction
    data object SaveStarted : SelectTokensNumberAction
    data object SaveSucceeded : SelectTokensNumberAction
    data object SaveFailed : SelectTokensNumberAction
}

fun reduceTokensNumber(state: SelectTokensNumberUiState, action: SelectTokensNumberAction): SelectTokensNumberUiState =
    when (action) {
        is SelectTokensNumberAction.Initialize -> state.copy(
            minimum = action.minimum,
            maximum = action.maximum,
            count = (state.count ?: action.count).coerceIn(action.minimum, action.maximum),
        )
        is SelectTokensNumberAction.Select -> if (state.editable && action.count in state.minimum..state.maximum) {
            state.copy(count = action.count, save = SaveState.IDLE)
        } else state
        SelectTokensNumberAction.SaveStarted -> state.copy(save = SaveState.SAVING)
        SelectTokensNumberAction.SaveSucceeded -> state.copy(save = SaveState.SAVED)
        SelectTokensNumberAction.SaveFailed -> state.copy(save = SaveState.ERROR)
    }
