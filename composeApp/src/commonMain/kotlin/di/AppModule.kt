package di

import data.repository.StickerRepositoryImpl
import data.remote.SetikerApiService
import data.remote.StickerApiRepository
import domain.repository.StickerRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import presentation.home.HomeViewModel
import presentation.packdetail.PackDetailViewModel
import presentation.createpack.CreatePackViewModel
import presentation.editor.EditorViewModel
import presentation.crop.CropViewModel
import presentation.backgroundremover.BackgroundRemoverViewModel

expect fun platformModule(): Module

val appModule = module {
    includes(platformModule())

    // Repository
    singleOf(::StickerRepositoryImpl) bind StickerRepository::class
    single { SetikerApiService() }
    single { StickerApiRepository(api = get(), fileStorage = get()) }

    // ViewModels
    factoryOf(::HomeViewModel)
    factory {
        PackDetailViewModel(
            repository = get(),
            packActions = get(),
            fileStorage = get()
        )
    }
    factory { params ->
        CreatePackViewModel(
            repository = get(),
            fileStorage = get(),
            apiRepository = get()
        )
    }
    factory { params ->
        EditorViewModel(
            repository = get(),
            emojiPreferences = get(),
            fileStorage = get()
        )
    }
    factoryOf(::CropViewModel)
    factory {
        BackgroundRemoverViewModel(
            apiRepository = get()
        )
    }
}
