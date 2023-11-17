package com.cerebus.tokens.presentation.tokens_screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cerebus.tokens.data.EffectsRepositoryImpl
import com.cerebus.tokens.data.TokensRepositoryImpl
import com.cerebus.tokens.domain.repository.EffectsRepository
import com.cerebus.tokens.domain.repository.TokensRepository
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
import com.cerebus.tokens.presentation.settings_screen.SettingsViewModelFactory

class TokensViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val tokensRepository: TokensRepository = TokensRepositoryImpl(context.getSharedPreferences(
        SettingsViewModelFactory.TOKENS_PREFERENCES, Context.MODE_PRIVATE))
    private val effectsRepository: EffectsRepository = EffectsRepositoryImpl(context.getSharedPreferences(
        SettingsViewModelFactory.WIN_EFFECTS_PREFERENCES, Context.MODE_PRIVATE
    ))

    /** tokens **/
    private val getCheckedColorUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetCheckedColorUseCase(tokensRepository)
    }

    private val changeTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ChangeTokensNumberUseCase(tokensRepository)
    }

    private val clearAllTokensUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ClearAllTokensUseCase(tokensRepository)
    }

    private val checkTokenUseCase by lazy(LazyThreadSafetyMode.NONE) {
        CheckTokenUseCase(tokensRepository)
    }

    private val uncheckTokenUseCase by lazy(LazyThreadSafetyMode.NONE) {
        UncheckTokenUseCase(tokensRepository)
    }

    private val getTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetTokensNumberUseCase(tokensRepository)
    }

    private val checkTokensAreGrappedUseCase by lazy(LazyThreadSafetyMode.NONE) {
        CheckTokensAreGrappedUseCase(tokensRepository)
    }

    private val getAllTokensUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetAllTokensUseCase(tokensRepository)
    }

    private val getMinTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMinTokensNumberUseCase(tokensRepository)
    }

    private val getMaxTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMaxTokensNumberUseCase(tokensRepository)
    }

    /** effects **/

    private val isWinAnimationOnUseCase by lazy(LazyThreadSafetyMode.NONE) {
        IsWinAnimationOnUseCase(effectsRepository)
    }

    private val isWinSoundOnUseCase by lazy(LazyThreadSafetyMode.NONE) {
        IsWinSoundOnUseCase(effectsRepository)
    }

    private val getEffectsDurationUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetEffectsDurationUseCase(effectsRepository)
    }

    private val getAnimationRepeatTimesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetAnimationRepeatTimesUseCase(effectsRepository)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TokensViewModel(
            getCheckedColorUseCase = getCheckedColorUseCase,
            changeTokensNumberUseCase = changeTokensNumberUseCase,
            clearAllTokensUseCase = clearAllTokensUseCase,
            checkTokenUseCase = checkTokenUseCase,
            uncheckTokenUseCase = uncheckTokenUseCase,
            getTokensNumberUseCase = getTokensNumberUseCase,
            checkTokensAreGrappedUseCase = checkTokensAreGrappedUseCase,
            getAllTokensUseCase = getAllTokensUseCase,
            getMinTokensNumberUseCase = getMinTokensNumberUseCase,
            getMaxTokensNumberUseCase = getMaxTokensNumberUseCase,

            isWinAnimationOnUseCase = isWinAnimationOnUseCase,
            isWinSoundOnUseCase = isWinSoundOnUseCase,
            getEffectsDurationUseCase = getEffectsDurationUseCase,
            getAnimationRepeatTimesUseCase = getAnimationRepeatTimesUseCase
        ) as T
    }
}
