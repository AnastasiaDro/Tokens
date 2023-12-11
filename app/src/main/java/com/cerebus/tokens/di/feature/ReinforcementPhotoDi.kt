package com.cerebus.tokens.di.feature

import com.cerebus.tokens.reinforcement_photo.data.GalleryRepositoryImpl
import com.cerebus.tokens.reinforcement_photo.data.storage.GalleryStorage
import com.cerebus.tokens.reinforcement_photo.data.storage.GalleryStorageImpl
import com.cerebus.tokens.reinforcement_photo.domain.repository.GalleryRepository
import com.cerebus.tokens.reinforcement_photo.domain.usecases.GetSelectedPhotoUriUseCase
import com.cerebus.tokens.reinforcement_photo.domain.usecases.SaveSelectedPhotoUriUseCase
import com.cerebus.tokens.reinforcement_photo.presentation.ChangePhotoViewModel
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

val reinforcementModule = module {

    /** presentation **/
    viewModel<ChangePhotoViewModel> {
        ChangePhotoViewModel(
            getSelectedPhotoUriUseCase = get(),
            saveSelectedPhotoUriUseCase = get()
        )
    }

    /** domain **/
    factory<GetSelectedPhotoUriUseCase>{
        GetSelectedPhotoUriUseCase(repository = get())
    }
    factory<SaveSelectedPhotoUriUseCase> {
        SaveSelectedPhotoUriUseCase(repository = get())
    }

    single<GalleryRepository> {
        GalleryRepositoryImpl(galleryStorage = get())
    }

    /** data **/
    single<GalleryStorage> {
        GalleryStorageImpl(context = get(), loggerFactory = get())
    }
}