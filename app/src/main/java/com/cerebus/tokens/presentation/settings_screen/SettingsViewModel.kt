package com.cerebus.tokens.presentation.settings_screen

import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.domain.usecases.effects.IsWinAnimationOnUseCase
import com.cerebus.tokens.domain.usecases.effects.IsWinSoundOnUseCase
import com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinAnimation
import com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinSound
import com.cerebus.tokens.domain.usecases.tokens.ChangeCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMaxTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMinTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetTokensNumberUseCase
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
    private val changeTokensNumberUseCase: com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase,
    private val getTokensNumberUseCase: GetTokensNumberUseCase,
    private val getMinTokensNumberUseCase: GetMinTokensNumberUseCase,
    private val getMaxTokensNumberUseCase: GetMaxTokensNumberUseCase,
    private val getChangeCheckedColorUseCase: com.cerebus.tokens.domain.usecases.tokens.ChangeCheckedColorUseCase,
    private val getCheckedColorUseCase: GetCheckedColorUseCase,

    private val isWinAnimationOnUseCase: com.cerebus.tokens.domain.usecases.effects.IsWinAnimationOnUseCase,
    private val isWinSoundOnUseCase: com.cerebus.tokens.domain.usecases.effects.IsWinSoundOnUseCase,
    private val plugOnOffWinAnimation: com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinAnimation,
    private val plugOnOffWinSound: com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinSound
) : ViewModel() {

    private val mutableColorLiveData = MutableLiveData(getTokensColor())
    val colorLiveData: LiveData<Int> = mutableColorLiveData

    private val changeColorSharedFlow = MutableSharedFlow<Boolean>()
    val changeColorFlow = changeColorSharedFlow.asSharedFlow()

    private val selectTokensNumberSharedFlow = MutableSharedFlow<Boolean>()
    val selectTokensNumberFlow = selectTokensNumberSharedFlow.asSharedFlow()

    private val changedTokensNumMutableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val changedTokensNumLiveData: LiveData<Boolean> = changedTokensNumMutableLiveData

    fun getIsAnimation() = isWinAnimationOnUseCase.execute()
    fun getIsSound() = isWinSoundOnUseCase.execute()

    fun changeAnimation(isAnimate: Boolean) {
        plugOnOffWinAnimation.execute(isAnimate)
    }

    fun changeSound(isSound: Boolean) {
        plugOnOffWinSound.execute(isSound)
    }

    fun askForChangeTokensColor() {
        viewModelScope.launch {
            changeColorSharedFlow.emit(true)
        }
    }

    fun askChangeTokensNumber() {
        viewModelScope.launch {
            selectTokensNumberSharedFlow.emit(true)
        }
    }

    fun changeTokensNum(newNum: Int) {
        changeTokensNumberUseCase.execute(newNum)
        changedTokensNumMutableLiveData.postValue(true)
    }

    fun getTokensNum() = getTokensNumberUseCase.execute()
    fun getMaxTokensNum() = getMaxTokensNumberUseCase.execute()
    fun getMinTokensNum() = getMinTokensNumberUseCase.execute()

    fun changeTokensColor(@ColorInt color: Int) {
        getChangeCheckedColorUseCase.execute(color)
        mutableColorLiveData.postValue(color)
    }

    @ColorInt
    fun getTokensColor() = getCheckedColorUseCase.execute()
}
