package com.cerebus.tokens.di

import com.cerebus.tokens.domain.usecases.tokens.CheckTokenUseCase
import com.cerebus.tokens.domain.usecases.tokens.CheckTokensAreGrappedUseCase
import com.cerebus.tokens.domain.usecases.tokens.ClearAllTokensUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetAllTokensUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMaxTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMinTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.UncheckTokenUseCase
import com.cerebus.tokens.presentation.settings_screen.SettingsViewModel
import com.cerebus.tokens.presentation.tokens_screen.TokensViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel<TokensViewModel> {
        TokensViewModel(
            getCheckedColorUseCase = get(),
            changeTokensNumberUseCase = get(),
            clearAllTokensUseCase = get(),
            checkTokenUseCase = get(),
            uncheckTokenUseCase = get(),
            getTokensNumberUseCase = get(),
            checkTokensAreGrappedUseCase = get(),
            getAllTokensUseCase = get(),
            getMinTokensNumberUseCase = get(),
            getMaxTokensNumberUseCase = get(),

            isWinAnimationOnUseCase = get(),
            isWinSoundOnUseCase = get(),
            getEffectsDurationUseCase = get(),
            getAnimationRepeatTimesUseCase = get()
        )
    }

    viewModel<SettingsViewModel> {
        SettingsViewModel(
            changeTokensNumberUseCase = get(),
            getTokensNumberUseCase = get(),
            getMinTokensNumberUseCase = get(),
            getMaxTokensNumberUseCase = get(),
            getChangeCheckedColorUseCase = get(),
            getCheckedColorUseCase = get(),
            isWinAnimationOnUseCase = get(),
            isWinSoundOnUseCase = get(),
            plugOnOffWinAnimationUseCase = get(),
            plugOnOffWinSoundUseCase = get(),
        )
    }
}