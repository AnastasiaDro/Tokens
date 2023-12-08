package com.cerebus.tokens.di.feature

import data.effects.EffectsRepositoryImpl
import data.effects.storage.EffectsStorage
import data.effects.storage.EffectsStorageImpl
import data.reinforcement.ReinforcementRepositoryImpl
import data.reinforcement.storage.ReinforcementStorage
import data.reinforcement.storage.ReinforcementStorageImpl
import data.tokens.TokensRepositoryImpl
import data.tokens.storage.TokensStorage
import data.tokens.storage.TokensStorageImpl
import domain.repository.EffectsRepository
import domain.repository.ReinforcementRepository
import domain.repository.TokensRepository
import domain.usecases.effects.GetAnimationRepeatTimesUseCase
import domain.usecases.effects.GetEffectsDurationUseCase
import domain.usecases.effects.IsWinAnimationOnUseCase
import domain.usecases.effects.IsWinSoundOnUseCase
import domain.usecases.effects.PlugOnOffWinAnimationUseCase
import domain.usecases.effects.PlugOnOffWinSoundUseCase
import domain.usecases.reinforcement.GetIsReinforcementShowUseCase
import domain.usecases.reinforcement.SetIsReinforcementShowUseCase
import domain.usecases.tokens.ChangeCheckedColorUseCase
import domain.usecases.tokens.ChangeTokensNumberUseCase
import domain.usecases.tokens.CheckTokenUseCase
import domain.usecases.tokens.CheckTokensAreGrappedUseCase
import domain.usecases.tokens.ClearAllTokensUseCase
import domain.usecases.tokens.GetAllTokensUseCase
import domain.usecases.tokens.GetCheckedColorUseCase
import domain.usecases.tokens.GetMaxTokensNumberUseCase
import domain.usecases.tokens.GetMinTokensNumberUseCase
import domain.usecases.tokens.GetTokensNumberUseCase
import domain.usecases.tokens.UncheckTokenUseCase
import presentation.settings_screen.SettingsViewModel
import presentation.tokens_screen.TokensViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val tokensFeatureModule = module {

    /** presentation **/
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
            getAnimationRepeatTimesUseCase = get(),

            getIsReinforcementShowUseCase = get()
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

            getIsReinforcementShowUseCase = get(),
            setIsReinforcementShowUseCase = get()
        )
    }



    /** Domain **/
    /* Tokens */
    factory<GetCheckedColorUseCase> {
        GetCheckedColorUseCase(tokensRepository = get())
    }

    factory<ChangeTokensNumberUseCase> {
        ChangeTokensNumberUseCase(tokensRepository = get())
    }

    factory<ClearAllTokensUseCase> {
        ClearAllTokensUseCase(tokensRepository = get())
    }

    factory<CheckTokenUseCase> {
        CheckTokenUseCase(tokensRepository = get())
    }

    factory<UncheckTokenUseCase> {
        UncheckTokenUseCase(tokensRepository = get())
    }

    factory<GetTokensNumberUseCase> {
        GetTokensNumberUseCase(tokensRepository = get())
    }

    factory<CheckTokensAreGrappedUseCase> {
        CheckTokensAreGrappedUseCase(tokensRepository = get())
    }

    factory<GetAllTokensUseCase> {
        GetAllTokensUseCase(tokensRepository = get())
    }

    factory<GetMinTokensNumberUseCase> {
        GetMinTokensNumberUseCase(tokensRepository = get())
    }

    factory<GetMaxTokensNumberUseCase> {
        GetMaxTokensNumberUseCase(tokensRepository = get())
    }

    factory<ChangeCheckedColorUseCase> {
        ChangeCheckedColorUseCase(tokensRepository = get())
    }


    /* Effects */
    factory<IsWinAnimationOnUseCase> {
        IsWinAnimationOnUseCase(effectsRepository = get())
    }

    factory<IsWinSoundOnUseCase> {
        IsWinSoundOnUseCase(effectsRepository = get())
    }

    factory<GetEffectsDurationUseCase> {
        GetEffectsDurationUseCase(effectsRepository = get())
    }

    factory<GetAnimationRepeatTimesUseCase> {
        GetAnimationRepeatTimesUseCase(effectsRepository = get())
    }


    factory<PlugOnOffWinAnimationUseCase> {
        PlugOnOffWinAnimationUseCase(effectsRepository = get())
    }

    factory<PlugOnOffWinSoundUseCase> {
        PlugOnOffWinSoundUseCase(effectsRepository = get())
    }

    /* Reinforcement settings */
    factory<GetIsReinforcementShowUseCase> {
        GetIsReinforcementShowUseCase(reinforcementRepository = get())
    }

    factory<SetIsReinforcementShowUseCase> {
        SetIsReinforcementShowUseCase(reinforcementRepository = get())
    }

    /** Data **/
    /* Tokens */
    single<TokensStorage> {
        TokensStorageImpl(context = get(), loggerFactory = get())
    }

    single<TokensRepository> {
        TokensRepositoryImpl(tokensStorage = get())
    }

    /* Effects */
    single<EffectsStorage> {
        EffectsStorageImpl(context = get(), loggerFactory = get())
    }

    single<EffectsRepository> {
        EffectsRepositoryImpl(effectsStorage = get())
    }

    /* Reinforcement */
    single<ReinforcementRepository> {
        ReinforcementRepositoryImpl(reinforcementStorage = get())
    }

    single<ReinforcementStorage> {
        ReinforcementStorageImpl(context = get(), loggerFactory = get())
    }
}