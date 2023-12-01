package com.cerebus.tokens.di

import com.cerebus.tokens.data.EffectsRepositoryImpl
import com.cerebus.tokens.data.TokensRepositoryImpl
import com.cerebus.tokens.data.storage.EffectsStorage
import com.cerebus.tokens.data.storage.EffectsStorageImpl
import com.cerebus.tokens.data.storage.TokensStorage
import com.cerebus.tokens.data.storage.TokensStorageImpl
import com.cerebus.tokens.domain.repository.EffectsRepository
import com.cerebus.tokens.domain.repository.TokensRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


val dataModule = module {

    //Tokens
    single<TokensStorage> {
        TokensStorageImpl(context = get()) //контекст встроен в коин, а что делать с префами?
    }

    single<TokensRepository> {
        TokensRepositoryImpl(tokensStorage = get())
    }

    //Effects
    single<EffectsStorage> {
        EffectsStorageImpl(context = get())
    }

    single<EffectsRepository> {
        EffectsRepositoryImpl(effectsStorage = get())
    }
}