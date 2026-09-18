package com.cerebus.tokens.di.feature

import data.effects.DataStoreWinEffectsRepository
import data.tokens.DataStoreTokenBoardRepository
import domain.repository.TokenBoardRepository
import domain.repository.WinEffectsRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import presentation.settings_screen.SettingsViewModel
import presentation.settings_screen.SelectColorViewModel
import presentation.tokens_screen.SelectTokensNumberViewModel
import presentation.tokens_screen.TokensViewModel

val tokensFeatureModule = module {
    single<TokenBoardRepository> { DataStoreTokenBoardRepository(get<android.content.Context>()) }
    single<WinEffectsRepository> { DataStoreWinEffectsRepository(get<android.content.Context>()) }
    viewModel { TokensViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { SelectTokensNumberViewModel(get()) }
    viewModel { SelectColorViewModel(get()) }
}
