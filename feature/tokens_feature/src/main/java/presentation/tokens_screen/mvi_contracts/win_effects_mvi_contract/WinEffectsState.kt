package presentation.tokens_screen.mvi_contracts.win_effects_mvi_contract

import presentation.tokens_screen.TokensFragment
import presentation.tokens_screen.TokensViewModel
import presentation.tokens_screen.mvi_contracts.Event

/**
 * [WinEffectsState] - a state which describes effects displaying
 * at the [TokensFragment] in custom MVI approach
 * is sent by [TokensViewModel]
 *
 * @see TokensFragment
 * @see TokensViewModel
 * @see WinEffectsEvent
 * @see Event
 *
 * @author Anastasia Drogunova
 * @since 17.01.2024
 */
data class WinEffectsState(
    val isAnimationRunning: Boolean,
    val isSoundPlaying: Boolean,
)