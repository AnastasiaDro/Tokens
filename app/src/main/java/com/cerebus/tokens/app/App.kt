package com.cerebus.tokens.app

import android.app.Application
import com.cerebus.tokens.di.dataModule
import com.cerebus.tokens.di.domainModule
import com.cerebus.tokens.di.presentationModule
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
            modules(presentationModule, domainModule, dataModule)
        }
    }
}