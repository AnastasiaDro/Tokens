package com.cerebus.tokens.di.feature

import com.cerebus.tokens.reinforcement_photo.presentation.ChangePhotoViewModel
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

val reinforcementModule = module {

    /** presentation **/
    viewModel<ChangePhotoViewModel> {
        ChangePhotoViewModel()
    }


}