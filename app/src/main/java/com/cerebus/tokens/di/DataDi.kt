package com.cerebus.tokens.di

import com.cerebus.tokens.data.effects_data.EffectsRepositoryImpl
import com.cerebus.tokens.tokens_data.TokensRepositoryImpl
import com.cerebus.tokens.data.effects_data.storage.EffectsStorage
import com.cerebus.tokens.data.effects_data.storage.EffectsStorageImpl
import com.cerebus.tokens.tokens_data.TokensStorage
import com.cerebus.tokens.tokens_data.TokensStorageImpl
import com.cerebus.tokens.domain.repository.EffectsRepository
import com.cerebus.tokens.domain.repository.TokensRepository
import org.koin.dsl.module


val dataModule = module {

    //Tokens
    single<com.cerebus.tokens.tokens_data.TokensStorage> {
        com.cerebus.tokens.tokens_data.TokensStorageImpl(context = get()) //контекст встроен в коин, а что делать с префами?
    }

    single<TokensRepository> {
        com.cerebus.tokens.tokens_data.TokensRepositoryImpl(tokensStorage = get())
    }

    //Effects
    single<EffectsStorage> {
        EffectsStorageImpl(context = get())
    }

    single<EffectsRepository> {
        EffectsRepositoryImpl(effectsStorage = get())
    }
}