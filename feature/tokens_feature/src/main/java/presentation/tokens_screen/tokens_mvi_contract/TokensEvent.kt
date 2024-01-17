package presentation.tokens_screen.tokens_mvi_contract

import presentation.tokens_screen.TokensFragment
import presentation.tokens_screen.TokensViewModel

/**
 * [TokensEvent] - an interface for user interaction with tokens
 * in custom MVI approach
 * used in [TokensFragment] which sends it to the [TokensViewModel]
 *
 * @see TokensFragment
 * @see TokensViewModel
 *
 * @author Anastasia Drogunova
 * @since 17.01.2024
 */
interface TokensEvent

class CheckTokenEvent(val index: Int) : TokensEvent

class UncheckTokenEvent(val index: Int) : TokensEvent

class GetTokensStateEvent() : TokensEvent

class ClearTokensEvent(): TokensEvent
