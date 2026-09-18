package com.cerebus.tokens.reinforcement_photo.di

import com.cerebus.tokens.reinforcement_photo.ReinforcementPhotoMediatorImpl
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import com.cerebus.tokens.reinforcement_photo.presentation.ChangePhotoViewModel
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

val reinforcementModule = module {
    single<ReinforcementPhotoMediator> { ReinforcementPhotoMediatorImpl() }
    viewModel { ChangePhotoViewModel(get()) }
}
