package com.cerebus.tokens.di.data

import com.cerebus.tokens.data.tokens_data.TokensRepositoryImpl
import com.cerebus.tokens.data.tokens_data.storage.TokensStorage
import com.cerebus.tokens.data.tokens_data.storage.TokensStorageImpl
import com.cerebus.tokens.domain.repository.TokensRepository
import org.koin.dsl.module

val tokensDataModule = module {

    single<TokensStorage> {
        TokensStorageImpl(context = get()) //контекст встроен в коин, а что делать с префами?
    }

    single<TokensRepository> {
        TokensRepositoryImpl(tokensStorage = get())
    }
}