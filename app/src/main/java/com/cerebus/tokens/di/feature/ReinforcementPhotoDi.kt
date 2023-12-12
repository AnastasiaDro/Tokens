package com.cerebus.tokens.di.feature

import com.cerebus.tokens.reinforcement_photo.data.ReinforcementGalleryRepositoryImpl
import com.cerebus.tokens.reinforcement_photo.domain.repository.ReinforcementGalleryRepository
import com.cerebus.tokens.reinforcement_photo.domain.usecases.GetSelectedPhotoPathUseCase
import com.cerebus.tokens.reinforcement_photo.domain.usecases.SaveSelectedPhotoPathUseCase
import com.cerebus.tokens.reinforcement_photo.presentation.ChangePhotoViewModel
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

val reinforcementModule = module {

    /** presentation **/
    viewModel<ChangePhotoViewModel> {
        ChangePhotoViewModel(
            getSelectedPhotoPathUseCase = get(),
            saveSelectedPhotoPathUseCase = get()
        )
    }

    /** domain **/
    factory<GetSelectedPhotoPathUseCase>{
        GetSelectedPhotoPathUseCase(repository = get())
    }
    factory<SaveSelectedPhotoPathUseCase> {
        SaveSelectedPhotoPathUseCase(repository = get())
    }

    single<ReinforcementGalleryRepository> {
        ReinforcementGalleryRepositoryImpl(reinforcementStorage = get())
    }
}