package com.cerebus.tokens.di.data

import com.cerebus.tokens.data.reinforcement.DataStoreReinforcementRepository
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import org.koin.dsl.module

val reinforcementDataModule = module {
    single<ReinforcementRepository> { DataStoreReinforcementRepository(get()) }
}
