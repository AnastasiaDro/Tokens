package com.cerebus.tokens.presentation.settings_screen

import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.domain.usecases.tokens.ChangeCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMaxTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMinTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetTokensNumberUseCase
import com.cerebus.tokens.effects_manager.EffectsManager
import com.cerebus.tokens.effects_manager.EffectsManagerImpl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * [SettingsViewModel] - a view model for
 * [SettingsFragment] screen
 * It communicates with domain layer and helps to set
 * app preferences and displaying of app preferences
 *
 * @see SettingsFragment
 *
 * @author Anastasia Drogunova
 * @since 28.04.2023
 */
class SettingsViewModel(
    private val changeTokensNumberUseCase: ChangeTokensNumberUseCase,
    private val getTokensNumberUseCase: GetTokensNumberUseCase,
    private val getMinTokensNumberUseCase: GetMinTokensNumberUseCase,
    private val getMaxTokensNumberUseCase: GetMaxTokensNumberUseCase,
    private val getChangeCheckedColorUseCase: ChangeCheckedColorUseCase,
    private val getCheckedColorUseCase: GetCheckedColorUseCase
) : ViewModel() {

    private val effectsManager: EffectsManager? = EffectsManagerImpl.getInstance()

    private val mutableColorLiveData = MutableLiveData(getTokensColor())
    val colorLiveData: LiveData<Int> = mutableColorLiveData

    private val changeColorSharedFlow = MutableSharedFlow<Boolean>()
    val changeColorFlow = changeColorSharedFlow.asSharedFlow()

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
        viewModelScope.launch {
            changeColorSharedFlow.emit(true)
        }
    }

    fun changeTokensNum(newNum: Int) {
        changeTokensNumberUseCase.execute(newNum)
        changedTokensNumMutableLiveData.postValue(true)
    }

    fun getTokensNum() = getTokensNumberUseCase.execute()
    fun getMaxTokensNumber() = getMaxTokensNumberUseCase.execute()
    fun getMinTokensNumber() = getMinTokensNumberUseCase.execute()

    fun changeTokensColor(@ColorInt color: Int) {
        getChangeCheckedColorUseCase.execute(color)
        mutableColorLiveData.postValue(color)
    }

    @ColorInt
    fun getTokensColor() = getCheckedColorUseCase.execute()
}
