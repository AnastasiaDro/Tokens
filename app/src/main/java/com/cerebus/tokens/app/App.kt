package com.cerebus.tokens.app

import android.app.Application
import com.cerebus.tokens.di.core.loggerModule
import com.cerebus.tokens.di.data.reinforcementDataModule
import com.cerebus.tokens.di.feature.reinforcementModule
import com.cerebus.tokens.di.feature.tokensFeatureModule
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
            modules(tokensFeatureModule, reinforcementModule, loggerModule, reinforcementDataModule)
        }
    }
}