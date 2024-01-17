package presentation.tokens_screen.mvi_contracts.win_effects_mvi_contract

import presentation.tokens_screen.mvi_contracts.Event

interface WinEffectsEvent : Event

class AskWinEffectsStateEvent() : WinEffectsEvent