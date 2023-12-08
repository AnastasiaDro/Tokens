package com.cerebus.tokens.di.core

import com.cerebus.tokens.logger.api.LoggerFactory
import com.cerebus.tokens.logger.impl.LoggerFactoryImpl
import org.koin.dsl.module

val loggerModule = module {
    single<LoggerFactory>  {
        LoggerFactoryImpl()
    }
}