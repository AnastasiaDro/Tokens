package com.cerebus.tokens.di.data

import com.cerebus.tokens.data.reinforcement.storage.ReinforcementStorage
import com.cerebus.tokens.data.reinforcement.storage.ReinforcementStorageImpl
import org.koin.dsl.module

val reinforcementDataModule = module {

    /** storage **/
    single<ReinforcementStorage> {
        ReinforcementStorageImpl(context = get(), loggerFactory = get())
    }
}