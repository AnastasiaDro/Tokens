package presentation.tokens_screen

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.models.Token
import domain.usecases.effects.GetEffectsDurationUseCase
import domain.usecases.effects.IsWinAnimationOnUseCase
import domain.usecases.effects.IsWinSoundOnUseCase
import domain.usecases.reinforcement.GetIsReinforcementShowUseCase
import domain.usecases.reinforcement.GetReinforcementUriStringUseCase
import domain.usecases.tokens.CheckTokenUseCase
import domain.usecases.tokens.CheckTokensAreGrappedUseCase
import domain.usecases.tokens.ClearAllTokensUseCase
import domain.usecases.tokens.GetAllTokensUseCase
import domain.usecases.tokens.GetMaxTokensNumberUseCase
import domain.usecases.tokens.GetMinTokensNumberUseCase
import domain.usecases.tokens.GetTokensNumberUseCase
import domain.usecases.tokens.UncheckTokenUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import presentation.tokens_screen.mvi_contracts.CommonEvent
import presentation.tokens_screen.mvi_contracts.Event
import presentation.tokens_screen.mvi_contracts.InitEvent
import presentation.tokens_screen.mvi_contracts.reinforcement_image_mvi_contract.GetReinforcementStateEvent
import presentation.tokens_screen.mvi_contracts.reinforcement_image_mvi_contract.ReinforcementEvent
import presentation.tokens_screen.mvi_contracts.reinforcement_image_mvi_contract.ReinforcementState
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.CheckTokenEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.ClearTokensEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.GetTokensStateEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.TokensEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.TokensState
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.UncheckTokenEvent
import presentation.tokens_screen.mvi_contracts.win_effects_mvi_contract.WinEffectsState

/**
 * [TokensViewModel] - a view model for
 * [TokensFragment] screen
 * It communicates with domain layer and helps to set
 * tokens views displaying
 *
 * @see TokensFragment
 *
 * @author Anastasia Drogunova
 * @since 28.04.2023
 */
class TokensViewModel(
    private val clearAllTokensUseCase: ClearAllTokensUseCase,
    private val checkTokenUseCase: CheckTokenUseCase,
    private val uncheckTokenUseCase: UncheckTokenUseCase,
    private val getTokensNumberUseCase: GetTokensNumberUseCase,
    private val checkTokensAreGrappedUseCase: CheckTokensAreGrappedUseCase,
    private val getAllTokensUseCase: GetAllTokensUseCase,
    private val getMinTokensNumberUseCase: GetMinTokensNumberUseCase,
    private val getMaxTokensNumberUseCase: GetMaxTokensNumberUseCase,

    private val isWinAnimationOnUseCase: IsWinAnimationOnUseCase,
    private val isWinSoundOnUseCase: IsWinSoundOnUseCase,
    private val getEffectsDurationUseCase: GetEffectsDurationUseCase,

    private val getIsReinforcementShowUseCase: GetIsReinforcementShowUseCase,
    private val getReinforcementUriStringUseCase: GetReinforcementUriStringUseCase
    ) : ViewModel() {



    /** Tokens **/
    private val tokensStateSharedFlow: MutableSharedFlow<TokensState> = MutableSharedFlow()
    val tokensStateFlow: SharedFlow<TokensState> = tokensStateSharedFlow

    /** Animation and sound **/
    private var isAnimationRunning = false
    private var isSoundPlaying = false
    private val winEffectsStateSharedFlow: MutableSharedFlow<WinEffectsState> = MutableSharedFlow()
    val winEffectsFlow: SharedFlow<WinEffectsState> = winEffectsStateSharedFlow

    /** Navigation **/
    private val navigateToSettingsMutableFlow: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val navigateToSettingsFlow = navigateToSettingsMutableFlow.asSharedFlow()



    /** Reinforcement **/
    private val reinforcementMutableStateFlow: MutableStateFlow<ReinforcementState> = MutableStateFlow(
        ReinforcementState(
            isReinforcementShow = getIsReinforcementShowUseCase.execute(),
            reinforcementImageUri = getReinforcementUriStringUseCase.execute()?.toUri()
        )
    )
    val reinforcementStateFlow: StateFlow<ReinforcementState> = reinforcementMutableStateFlow

    fun getTokensNum() = getTokensNumberUseCase.execute()

    private fun getTokensList(): List<Token> = getAllTokensUseCase.execute()

    private fun sendTokensState() {
        viewModelScope.launch { tokensStateSharedFlow.emit(TokensState(getTokensList())) }
    }
    private fun sendReinforcementState() {
        viewModelScope.launch {
            reinforcementMutableStateFlow.emit(
                ReinforcementState(
                    isReinforcementShow = getIsReinforcementShowUseCase.execute(),
                    reinforcementImageUri = getReinforcementUriStringUseCase.execute()?.toUri()
                )
            )
        }
    }

    fun updateTokensNum() {
        viewModelScope.launch { tokensStateSharedFlow.emit(TokensState(getTokensList())) }
    }

    fun clearTokens() {
        clearAllTokensUseCase.execute()
        viewModelScope.launch { tokensStateSharedFlow.emit(TokensState(getTokensList())) }
    }

    fun getMinTokensNum() = getMinTokensNumberUseCase.execute()
    fun getMaxTokensNum() = getMaxTokensNumberUseCase.execute()

    fun onSettingsPressed() {
        viewModelScope.launch {
            navigateToSettingsMutableFlow.emit(true)
        }
    }

    fun sendEvent(event: Event) {
        when(event) {
            is TokensEvent -> parseTokensEvent(event)
            is ReinforcementEvent -> parseReinforcementEvent(event)
            is CommonEvent -> parseCommonEvent(event)
        }
    }

    private fun parseTokensEvent(event: TokensEvent) {
        when(event) {
            is CheckTokenEvent -> onTokenSelected(event.index)
            is UncheckTokenEvent -> onTokenUnselected(event.index)
            is GetTokensStateEvent -> sendTokensState()
            is ClearTokensEvent -> clearTokens()
        }
    }

    private fun parseReinforcementEvent(event: ReinforcementEvent) {
        when(event) {
            is GetReinforcementStateEvent -> sendReinforcementState()
        }
    }

    private fun parseCommonEvent(event: CommonEvent) {
        when(event) {
            is InitEvent -> {
                sendTokensState()
                sendReinforcementState()
            }
        }
    }

    private fun onTokenSelected(tokenIndex: Int) {

        if (checkTokenUseCase.execute(tokenIndex)) {
            viewModelScope.launch { tokensStateSharedFlow.emit(TokensState(getTokensList())) }
        }

        if (checkTokensAreGrappedUseCase.execute()) {
            viewModelScope.launch {
                winEffectsStateSharedFlow.emit(
                    WinEffectsState(
                        isAnimationRunning = !isAnimationRunning && isWinAnimationOnUseCase.execute(),
                        isSoundPlaying = !isSoundPlaying && isWinSoundOnUseCase.execute()
                    )
                )
                isAnimationRunning = true
                isSoundPlaying = true

                delay(getEffectsDurationUseCase.execute())
                winEffectsStateSharedFlow.emit(
                    WinEffectsState(
                        isAnimationRunning = false,
                        isSoundPlaying = false
                    )
                )
                isAnimationRunning = false
                isSoundPlaying = false
            }
        }
    }
    private fun onTokenUnselected(tokenIndex: Int) {
        if (uncheckTokenUseCase.execute(tokenIndex))
            viewModelScope.launch { tokensStateSharedFlow.emit(TokensState(getTokensList())) }
    }
}
