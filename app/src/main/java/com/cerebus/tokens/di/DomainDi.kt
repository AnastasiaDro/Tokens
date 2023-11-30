package com.cerebus.tokens.di

import com.cerebus.tokens.domain.usecases.effects.GetAnimationRepeatTimesUseCase
import com.cerebus.tokens.domain.usecases.effects.GetEffectsDurationUseCase
import com.cerebus.tokens.domain.usecases.effects.IsWinAnimationOnUseCase
import com.cerebus.tokens.domain.usecases.effects.IsWinSoundOnUseCase
import com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinAnimationUseCase
import com.cerebus.tokens.domain.usecases.effects.PlugOnOffWinSoundUseCase
import com.cerebus.tokens.domain.usecases.tokens.ChangeCheckedColorUseCase
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
import org.koin.dsl.module

val domainModule = module {

    /** Tokens **/
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


    /** Effects **/

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
}