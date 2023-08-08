package com.cerebus.tokens.settings_screen

import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cerebus.tokens.R
import com.cerebus.tokens.effects_manager.EffectsManager
import com.cerebus.tokens.effects_manager.EffectsManagerImpl
import com.cerebus.tokens.tokens_manager.TokensManager
import com.cerebus.tokens.tokens_manager.TokensManagerImpl
import com.cerebus.tokens.utils.SingleLiveEvent

/**
 * [SettingsViewModel] - a view model for
 * [SettingsFragment] screen
 * It communicates with shared preferences and helps to set
 * app preferences and displaying of app preferences
 *
 * @see SettingsFragment
 *
 * @author Anastasia Drogunova
 * @since 28.04.2023
 */
class SettingsViewModel : ViewModel() {

    private val tokensManager: TokensManager? = TokensManagerImpl.getInstance()
    private val effectsManager: EffectsManager? = EffectsManagerImpl.getInstance()

    private val mutableColorLiveData = MutableLiveData(getTokensColor())
    val colorLiveData: LiveData<Int> = mutableColorLiveData

    private val changeColorMutableLiveData = SingleLiveEvent<Boolean>()
    val changeColorLiveData: LiveData<Boolean> = changeColorMutableLiveData

    private val changedTokensNumMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val changedTokensNumLiveData: LiveData<Boolean> = changedTokensNumMutableLiveData

    fun getIsAnimation() = effectsManager?.getIsAnimateWin() ?: false
    fun getIsSound() = effectsManager?.getIsSoundWin() ?: false

    fun changeAnimation(isAnimate: Boolean) {
        effectsManager?.setIsAnimateWin(isAnimate)
    }

    fun changeSound(isSound: Boolean) {
        effectsManager?.setIsSoundWin(isSound)
    }

    fun askForChangeTokensColor() {
        changeColorMutableLiveData.value = true
    }

    fun changeTokensNum(newNum: Int) {
        tokensManager?.let { manager ->
            manager.setTokensNum(newNum)
            if (newNum < manager.getCheckedTokensNumber()) manager.setCheckedTokensNumber(newNum)
            changedTokensNumMutableLiveData.postValue(true)
        }
    }

    fun getTokensNum() = tokensManager?.getTokensNum() ?: 1
    fun getMaxTokensNumber() = tokensManager?.getMaxTokensNum() ?: 1
    fun getMinTokensNumber() = tokensManager?.getMinTokensNum() ?: 1

    fun changeTokensColor(@ColorInt color: Int) {
        tokensManager?.setTokensColor(color)
        mutableColorLiveData.postValue(color)
    }

    @ColorInt
    fun getTokensColor() = tokensManager?.getTokensColor() ?: R.color.checkedColor
}
