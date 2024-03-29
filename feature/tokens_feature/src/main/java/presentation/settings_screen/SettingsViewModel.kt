package presentation.settings_screen

import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.usecases.effects.IsWinAnimationOnUseCase
import domain.usecases.effects.IsWinSoundOnUseCase
import domain.usecases.effects.PlugOnOffWinAnimationUseCase
import domain.usecases.effects.PlugOnOffWinSoundUseCase
import domain.usecases.reinforcement.GetIsReinforcementShowUseCase
import domain.usecases.reinforcement.SetIsReinforcementShowUseCase
import domain.usecases.tokens.ChangeCheckedColorUseCase
import domain.usecases.tokens.ChangeTokensNumberUseCase
import domain.usecases.tokens.GetCheckedColorUseCase
import domain.usecases.tokens.GetMaxTokensNumberUseCase
import domain.usecases.tokens.GetMinTokensNumberUseCase
import domain.usecases.tokens.GetTokensNumberUseCase
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
    private val getCheckedColorUseCase: GetCheckedColorUseCase,

    private val isWinAnimationOnUseCase: IsWinAnimationOnUseCase,
    private val isWinSoundOnUseCase: IsWinSoundOnUseCase,
    private val plugOnOffWinAnimationUseCase: PlugOnOffWinAnimationUseCase,
    private val plugOnOffWinSoundUseCase: PlugOnOffWinSoundUseCase,

    private val getIsReinforcementShowUseCase: GetIsReinforcementShowUseCase,
    private val setIsReinforcementShowUseCase: SetIsReinforcementShowUseCase
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

    fun getIsReinforcement() = getIsReinforcementShowUseCase.execute()

    fun changeAnimation(isAnimate: Boolean) {
        plugOnOffWinAnimationUseCase.execute(isAnimate)
    }

    fun changeSound(isSound: Boolean) {
        plugOnOffWinSoundUseCase.execute(isSound)
    }

    fun changeReinforcement(checked: Boolean) {
        setIsReinforcementShowUseCase.execute(checked)
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

    fun updateTokensNum() {
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
