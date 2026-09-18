package com.cerebus.tokens.reinforcement_photo.di

import com.cerebus.tokens.reinforcement_photo.ReinforcementPhotoMediatorImpl
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import com.cerebus.tokens.reinforcement_photo.presentation.ChangePhotoViewModel
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.android.ext.koin.androidContext
import com.cerebus.tokens.reinforcement_photo.PhotoFiles
import com.cerebus.tokens.reinforcement_photo.AndroidPhotoFiles

val reinforcementModule = module {
    single<ReinforcementPhotoMediator> { ReinforcementPhotoMediatorImpl() }
    single<PhotoFiles> { AndroidPhotoFiles(androidContext()) }
    viewModel { ChangePhotoViewModel(get(), get(), get()) }
}
