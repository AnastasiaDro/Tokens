package com.cerebus.tokens.feature.tokens_feature.di

import com.cerebus.tokens.feature.tokens_feature.TokensMediatorImpl
import com.cerebus.tokens.feature.tokens_feature.api.TokensMediator
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
    single<TokensMediator> { TokensMediatorImpl() }
    single<TokenBoardRepository> { DataStoreTokenBoardRepository(get<android.content.Context>()) }
    single<WinEffectsRepository> { DataStoreWinEffectsRepository(get<android.content.Context>()) }
    viewModel { TokensViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { SelectTokensNumberViewModel(get(), get()) }
    viewModel { SelectColorViewModel(get(), get()) }
}
