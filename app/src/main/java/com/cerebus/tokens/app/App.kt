package com.cerebus.tokens.app

import android.app.Application
import com.cerebus.tokens.di.appModule
import com.cerebus.tokens.di.data.effectsDataModule
import com.cerebus.tokens.di.data.tokensDataModule
import com.cerebus.tokens.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@App)
            modules(appModule, domainModule, effectsDataModule, tokensDataModule)
        }
    }
}