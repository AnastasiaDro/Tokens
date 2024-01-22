package presentation.tokens_screen.mvi_contracts

import presentation.tokens_screen.TokensFragment
import presentation.tokens_screen.TokensViewModel
import presentation.tokens_screen.mvi_contracts.reinforcement_image_mvi_contract.ReinforcementEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.TokensEvent
import presentation.tokens_screen.mvi_contracts.win_effects_mvi_contract.WinEffectsEvent

/**
 * [Event] - a base interface for all Tokens screen events
 * in custom MVI approach
 * used in [TokensFragment] which sends it to the [TokensViewModel]
 *
 * @see TokensFragment
 * @see TokensViewModel
 * @see TokensEvent
 * @see WinEffectsEvent
 * @see ReinforcementEvent
 *
 * @author Anastasia Drogunova
 * @since 17.01.2024
 */
interface Event