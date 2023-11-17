package com.cerebus.tokens.presentation.tokens_screen

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.domain.models.Token
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
import com.cerebus.tokens.effects_manager.EffectsManager
import com.cerebus.tokens.effects_manager.EffectsManagerImpl
import com.cerebus.tokens.navigator.Destinations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    private val changeTokensNumberUseCase: ChangeTokensNumberUseCase,
    private val clearAllTokensUseCase: ClearAllTokensUseCase,
    private val checkTokenUseCase: CheckTokenUseCase,
    private val uncheckTokenUseCase: UncheckTokenUseCase,
    private val getTokensNumberUseCase: GetTokensNumberUseCase,
    private val checkTokensAreGrappedUseCase: CheckTokensAreGrappedUseCase,
    private val getAllTokensUseCase: GetAllTokensUseCase,
    private val getMinTokensNumberUseCase: GetMinTokensNumberUseCase,
    private val getMaxTokensNumberUseCase: GetMaxTokensNumberUseCase
    ) : ViewModel() {


    private var effectsManager: EffectsManager? = null
    private var effectsDuration = DEFAULT_EFFECTS_DURATION
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

    private val navigateMutableLiveData: MutableLiveData<Destinations?> = MutableLiveData(null)
    val navigateLiveData: LiveData<Destinations?> = navigateMutableLiveData

    private val animationMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val animationLiveData: LiveData<Boolean> = animationMutableLiveData

    private val soundMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val soundLiveData: LiveData<Boolean> = soundMutableLiveData

    fun getTokensNum() = getTokensNumberUseCase.execute()

    fun getCheckedColor() = getCheckedColorUseCase.execute()

    fun getTokens(): Flow<Token> = getAllTokensUseCase.execute()

    fun initData(preferences: SharedPreferences) {
        effectsManager = EffectsManagerImpl.getInstance()
        if (effectsManager == null) effectsManager = EffectsManagerImpl.create(preferences)
        viewModelScope.launch(Dispatchers.IO) {
            effectsManager?.let { if (!it.isReady()) it.initEffectsManager() }
            prefsLoadedMutableLiveData.postValue(true)
        }
        navigateMutableLiveData.value = null
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

    fun onAboutAppPressed() {
        viewModelScope.launch {
            navigateMutableLiveData.postValue(Destinations.ABOUT_APP_FRAGMENT)
        }
    }

    fun onTokenSelected(tokenIndex: Int) {
            if (checkTokenUseCase.execute(tokenIndex))
                viewModelScope.launch { selectedMutableLiveData.postValue(tokenIndex) }
            if (checkTokensAreGrappedUseCase.execute()) {
                if (!isAnimationRunning && effectsManager?.getIsAnimateWin() == true) {
                    viewModelScope.launch {
                        isAnimationRunning = true
                        if (effectsManager?.getIsAnimateWin() == true) {
                            for (i in 0 until ANIMATION_REPEAT_TIMES) {
                                animationMutableLiveData.postValue(true)
                                delay(effectsDuration)
                            }
                            animationMutableLiveData.postValue(false)
                            isAnimationRunning = false
                        }
                    }
                }
                if (!isSoundPlaying && effectsManager?.getIsSoundWin() == true) {
                    viewModelScope.launch {
                        isSoundPlaying = true
                        soundMutableLiveData.postValue(true)
                        delay(effectsDuration)
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


    companion object {
        private const val ANIMATION_REPEAT_TIMES = 1
        private const val DEFAULT_EFFECTS_DURATION = 5000L
    }
}