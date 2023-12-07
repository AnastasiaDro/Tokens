package com.cerebus.tokens.di.data

import com.cerebus.tokens.data.effects_data.EffectsRepositoryImpl
import com.cerebus.tokens.data.effects_data.storage.EffectsStorage
import com.cerebus.tokens.data.effects_data.storage.EffectsStorageImpl
import com.cerebus.tokens.domain.repository.EffectsRepository
import org.koin.dsl.module


val effectsDataModule = module {

    single<EffectsStorage> {
        EffectsStorageImpl(context = get())
    }

    single<EffectsRepository> {
        EffectsRepositoryImpl(effectsStorage = get())
    }
}