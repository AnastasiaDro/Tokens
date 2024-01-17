package presentation.tokens_screen.mvi_contracts.tokens_mvi_contract

import domain.models.Token
import presentation.tokens_screen.TokensFragment
import presentation.tokens_screen.TokensViewModel
import presentation.tokens_screen.mvi_contracts.Event

/**
 * [TokensState] - a screen state which describes tokens displaying
 * at the [TokensFragment] in custom MVI approach
 * is sent by [TokensViewModel]
 *
 * @see TokensFragment
 * @see TokensViewModel
 * @see TokensEvent
 * @see Event
 *
 * @author Anastasia Drogunova
 * @since 17.01.2024
 */
data class TokensState(val tokens: List<Token>)

