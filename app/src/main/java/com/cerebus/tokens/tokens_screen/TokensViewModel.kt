package com.cerebus.tokens.tokens_screen

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.R
import com.cerebus.tokens.effects_manager.EffectsManager
import com.cerebus.tokens.effects_manager.EffectsManagerImpl
import com.cerebus.tokens.navigator.Destinations
import com.cerebus.tokens.tokens_manager.TokensManager
import com.cerebus.tokens.tokens_manager.TokensManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * [TokensViewModel] - a view model for
 * [TokensFragment] screen
 * It communicates with shared preferences and helps to set
 * tokens views displaying
 *
 * @see TokensFragment
 *
 * @author Anastasia Drogunova
 * @since 28.04.2023
 */
class TokensViewModel : ViewModel() {

    private var tokensManager: TokensManager? = null
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

    fun getTokensNum() = tokensManager?.getTokensNum() ?: 1
    fun getMaxTokensNumber() = tokensManager?.getMaxTokensNum() ?: 1
    fun getMinTokensNumber() = tokensManager?.getMinTokensNum() ?: 1

    fun getCheckedTokensNum() = tokensManager?.getCheckedTokensNumber() ?: 0
    fun getCheckedColor() = tokensManager?.getTokensColor() ?: R.color.checkedColor

    fun initData(preferences: SharedPreferences) {
        tokensManager = TokensManagerImpl.getInstance()
        effectsManager = EffectsManagerImpl.getInstance()
        if (tokensManager == null) tokensManager = TokensManagerImpl.create(preferences)
        if (effectsManager == null) effectsManager = EffectsManagerImpl.create(preferences)
        viewModelScope.launch(Dispatchers.IO) {
            tokensManager?.let { if (!it.isReady()) it.initTokensManager() }
            effectsManager?.let { if (!it.isReady()) it.initEffectsManager() }
            prefsLoadedMutableLiveData.postValue(true)
        }
        navigateMutableLiveData.value = null
    }

    fun changeTokensNum(newNum: Int) {
        tokensManager?.let { manager ->
            manager.setTokensNum(newNum)
            if (newNum < manager.getCheckedTokensNumber()) manager.setCheckedTokensNumber(newNum)
            changedTokensNumMutableLiveData.postValue(true)
        }
    }

    fun clearTokens() {
        tokensManager?.setCheckedTokensNumber(0)
        changeCheckedNumMutableLiveData.postValue(1)
    }

    fun onAboutAppPressed() {
        viewModelScope.launch {
            navigateMutableLiveData.postValue(Destinations.ABOUT_APP_FRAGMENT)
        }
    }

    fun onTokenSelected(tokenIndex: Int) {
        tokensManager?.let {
            if (it.increaseCheckedTokensNum())
                viewModelScope.launch { selectedMutableLiveData.postValue(tokenIndex) }
            if (it.getCheckedTokensNumber() == it.getTokensNum()) {
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
    }

    fun onTokenUnselected(tokenIndex: Int) {
        tokensManager?.let {
            if (it.decreaseCheckedTokensNum())
                viewModelScope.launch { unselectedMutableLiveData.postValue(tokenIndex) }
        }
    }


    companion object {
        private const val ANIMATION_REPEAT_TIMES = 1
        private const val DEFAULT_EFFECTS_DURATION = 5000L
    }
}