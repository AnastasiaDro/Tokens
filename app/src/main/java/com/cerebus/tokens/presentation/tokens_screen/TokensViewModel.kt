package com.cerebus.tokens.presentation.tokens_screen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.domain.models.Token
import com.cerebus.tokens.domain.usecases.effects.GetAnimationRepeatTimesUseCase
import com.cerebus.tokens.domain.usecases.effects.GetEffectsDurationUseCase
import com.cerebus.tokens.domain.usecases.effects.IsWinAnimationOnUseCase
import com.cerebus.tokens.domain.usecases.effects.IsWinSoundOnUseCase
import com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.CheckTokenUseCase
import com.cerebus.tokens.domain.usecases.tokens.CheckTokensAreGrappedUseCase
import com.cerebus.tokens.domain.usecases.tokens.ClearAllTokensUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetAllTokensUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMaxTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMinTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.UncheckTokenUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

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
    private val getCheckedColorUseCase: GetCheckedColorUseCase,
    private val changeTokensNumberUseCase: com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase,
    private val clearAllTokensUseCase: ClearAllTokensUseCase,
    private val checkTokenUseCase: CheckTokenUseCase,
    private val uncheckTokenUseCase: UncheckTokenUseCase,
    private val getTokensNumberUseCase: GetTokensNumberUseCase,
    private val checkTokensAreGrappedUseCase: CheckTokensAreGrappedUseCase,
    private val getAllTokensUseCase: GetAllTokensUseCase,
    private val getMinTokensNumberUseCase: GetMinTokensNumberUseCase,
    private val getMaxTokensNumberUseCase: GetMaxTokensNumberUseCase,

    private val isWinAnimationOnUseCase: com.cerebus.tokens.domain.usecases.effects.IsWinAnimationOnUseCase,
    private val isWinSoundOnUseCase: com.cerebus.tokens.domain.usecases.effects.IsWinSoundOnUseCase,
    private val getEffectsDurationUseCase: com.cerebus.tokens.domain.usecases.effects.GetEffectsDurationUseCase,
    private val getAnimationRepeatTimesUseCase: com.cerebus.tokens.domain.usecases.effects.GetAnimationRepeatTimesUseCase
    ) : ViewModel() {

    private var isAnimationRunning = false
    private var isSoundPlaying = false

    private val prefsLoadedMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val prefsLoadedLiveData: LiveData<Boolean> = prefsLoadedMutableLiveData

    private val selectedMutableLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val selectedLiveData: LiveData<Int> = selectedMutableLiveData

    private val unselectedMutableLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val unselectedLiveData: LiveData<Int> = unselectedMutableLiveData

    private val changeCheckedNumMutableLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val changeCheckedTokensNumLiveData: LiveData<Int> = changeCheckedNumMutableLiveData

    private val changedTokensNumMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val changedTokensNumLiveData: LiveData<Boolean> = changedTokensNumMutableLiveData

    private val navigateToSettingsMutableFlow: MutableSharedFlow<Boolean> = MutableSharedFlow(replay = 0, extraBufferCapacity = 0)
    val navigateToSettingsFlow = navigateToSettingsMutableFlow.asSharedFlow()

    private val animationMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val animationLiveData: LiveData<Boolean> = animationMutableLiveData

    private val soundMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val soundLiveData: LiveData<Boolean> = soundMutableLiveData

    fun getTokensNum() = getTokensNumberUseCase.execute()

    fun getCheckedColor() = getCheckedColorUseCase.execute()

    fun getTokens(): Flow<com.cerebus.tokens.domain.models.Token> = getAllTokensUseCase.execute()

    fun initData() {
        viewModelScope.launch(Dispatchers.IO) {
            prefsLoadedMutableLiveData.postValue(true)
        }
    }

    fun changeTokensNum(newNum: Int) {
        changeTokensNumberUseCase.execute(newNum)
        changedTokensNumMutableLiveData.postValue(true)
    }

    fun clearTokens() {
        clearAllTokensUseCase.execute()
        changeCheckedNumMutableLiveData.postValue(1)
    }

    fun getMinTokensNum() = getMinTokensNumberUseCase.execute()
    fun getMaxTokensNum() = getMaxTokensNumberUseCase.execute()

    fun onSettingsPressed() {
        viewModelScope.launch {
            navigateToSettingsMutableFlow.emit(true)
        }
    }

    fun onTokenSelected(tokenIndex: Int) {
            if (checkTokenUseCase.execute(tokenIndex))
                viewModelScope.launch { selectedMutableLiveData.postValue(tokenIndex) }
            if (checkTokensAreGrappedUseCase.execute()) {
                if (!isAnimationRunning && isWinAnimationOnUseCase.execute()) {
                    viewModelScope.launch {
                        isAnimationRunning = true
                        for (i in 0 until getAnimationRepeatTimesUseCase.execute()) {
                            animationMutableLiveData.postValue(true)
                            delay(getEffectsDurationUseCase.execute())
                        }
                        animationMutableLiveData.postValue(false)
                        isAnimationRunning = false
                    }
                }
                if (!isSoundPlaying && isWinSoundOnUseCase.execute()) {
                    viewModelScope.launch {
                        isSoundPlaying = true
                        soundMutableLiveData.postValue(true)
                        delay(getEffectsDurationUseCase.execute())
                        soundMutableLiveData.postValue(false)
                        isSoundPlaying = false
                    }
                }
            }
    }

    fun onTokenUnselected(tokenIndex: Int) {
        if (uncheckTokenUseCase.execute(tokenIndex))
            viewModelScope.launch { unselectedMutableLiveData.postValue(tokenIndex) }
    }



}