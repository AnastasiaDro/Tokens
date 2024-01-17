package presentation.tokens_screen.mvi_contracts.win_effects_mvi_contract

import presentation.tokens_screen.TokensFragment
import presentation.tokens_screen.TokensViewModel
import presentation.tokens_screen.mvi_contracts.Event

/**
 * [WinEffectsEvent] - an interface describes the displaying win effects
 * in custom MVI approach
 * will be using in [TokensFragment] which sends it to the [TokensViewModel]
 *
 * @see TokensFragment
 * @see TokensViewModel
 * @see WinEffectsState
 * @see Event
 *
 * @author Anastasia Drogunova
 * @since 17.01.2024
 */
interface WinEffectsEvent : Event

class AskWinEffectsStateEvent() : WinEffectsEvent