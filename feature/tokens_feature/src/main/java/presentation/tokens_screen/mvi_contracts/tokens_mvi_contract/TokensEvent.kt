package presentation.tokens_screen.mvi_contracts.tokens_mvi_contract

import presentation.tokens_screen.TokensFragment
import presentation.tokens_screen.TokensViewModel
import presentation.tokens_screen.mvi_contracts.Event

/**
 * [TokensEvent] - an interface for user interaction with tokens
 * in custom MVI approach
 * used in [TokensFragment] which sends it to the [TokensViewModel]
 *
 * @see TokensFragment
 * @see TokensViewModel
 * @see TokensState
 * @see Event
 *
 * @author Anastasia Drogunova
 * @since 17.01.2024
 */
interface TokensEvent : Event

class CheckTokenEvent(val index: Int) : TokensEvent

class UncheckTokenEvent(val index: Int) : TokensEvent

class GetTokensStateEvent() : TokensEvent

class ClearTokensEvent(): TokensEvent
