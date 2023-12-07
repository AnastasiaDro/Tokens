package com.cerebus.tokens.di

import settings_screen.SettingsViewModel
import tokens_screen.TokensViewModel
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