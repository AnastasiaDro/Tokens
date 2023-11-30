package com.cerebus.tokens.presentation.settings_screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cerebus.tokens.data.EffectsRepositoryImpl
import com.cerebus.tokens.data.TokensRepositoryImpl
import com.cerebus.tokens.data.storage.TokensStorage
import com.cerebus.tokens.data.storage.TokensStorageImpl
import com.cerebus.tokens.domain.repository.EffectsRepository
import com.cerebus.tokens.domain.repository.TokensRepository
import com.cerebus.tokens.data.storage.EffectsStorage
import com.cerebus.tokens.data.storage.EffectsStorageImpl
import com.cerebus.tokens.domain.usecases.effects.IsWinAnimationOnUseCase
import com.cerebus.tokens.domain.usecases.effects.IsWinSoundOnUseCase
import com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinAnimationUseCase
import com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinSoundUseCase
import com.cerebus.tokens.domain.usecases.tokens.ChangeCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMaxTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMinTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetTokensNumberUseCase

class SettingsViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val effectsStorage: EffectsStorage =
        EffectsStorageImpl(context)

    private val tokensStorage: TokensStorage =
        TokensStorageImpl(context)

    private val tokensRepository: TokensRepository = TokensRepositoryImpl(tokensStorage)
    private val effectsRepository: EffectsRepository = EffectsRepositoryImpl(effectsStorage)

    /** tokens **/
    private val changeTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ChangeTokensNumberUseCase(tokensRepository)
    }

    private val getTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetTokensNumberUseCase(tokensRepository)
    }

    private val getMinTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMinTokensNumberUseCase(tokensRepository)
    }

    private val getMaxTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMaxTokensNumberUseCase(tokensRepository)
    }

    private val getChangeCheckedColorUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ChangeCheckedColorUseCase(tokensRepository)
    }

    private val getCheckedColorUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetCheckedColorUseCase(tokensRepository)
    }

    /** effects **/

    private val isWinAnimationOnUseCase by lazy(LazyThreadSafetyMode.NONE) {
        IsWinAnimationOnUseCase(effectsRepository)
    }

    private val isWinSoundOnUseCase by lazy(LazyThreadSafetyMode.NONE) {
        IsWinSoundOnUseCase(effectsRepository)
    }

    private val plugOnOffWinAnimationUseCase by lazy(LazyThreadSafetyMode.NONE) {
        PlugOnOffWinAnimationUseCase(effectsRepository)
    }

    private val plugOnOffWinSoundUseCase by lazy(LazyThreadSafetyMode.NONE) {
        PlugOnOffWinSoundUseCase(effectsRepository)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            changeTokensNumberUseCase = changeTokensNumberUseCase,
            getTokensNumberUseCase = getTokensNumberUseCase,
            getMinTokensNumberUseCase = getMinTokensNumberUseCase,
            getMaxTokensNumberUseCase = getMaxTokensNumberUseCase,
            getChangeCheckedColorUseCase = getChangeCheckedColorUseCase,
            getCheckedColorUseCase = getCheckedColorUseCase,

            isWinAnimationOnUseCase = isWinAnimationOnUseCase,
            isWinSoundOnUseCase = isWinSoundOnUseCase,
            plugOnOffWinAnimationUseCase = plugOnOffWinAnimationUseCase,
            plugOnOffWinSoundUseCase = plugOnOffWinSoundUseCase

        ) as T
    }
}