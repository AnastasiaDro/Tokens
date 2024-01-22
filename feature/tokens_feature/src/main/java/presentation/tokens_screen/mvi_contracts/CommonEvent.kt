package presentation.tokens_screen.mvi_contracts

import presentation.tokens_screen.TokensFragment
import presentation.tokens_screen.TokensViewModel

/**
 * [CommonEvent] - an interface for events which could not be described by special event types
 * this event type is used in complex events which contains other simple ones
 * in custom MVI approach
 * used in [TokensFragment] which sends it to the [TokensViewModel]
 *
 * @see TokensFragment
 * @see TokensViewModel
 * @see Event
 *
 * @author Anastasia Drogunova
 * @since 17.01.2024
 */
interface CommonEvent : Event

class InitEvent : CommonEvent