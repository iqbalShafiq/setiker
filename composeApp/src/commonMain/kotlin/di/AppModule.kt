package di

import data.repository.StickerRepositoryImpl
import data.remote.SetikerApiService
import data.remote.StickerApiRepository
import domain.repository.StickerRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import presentation.home.HomeViewModel
import presentation.packdetail.PackDetailViewModel
import presentation.createpack.CreatePackViewModel
import presentation.editor.EditorViewModel
import presentation.crop.CropViewModel

expect fun platformModule(): Module

val appModule = module {
    includes(platformModule())

    // Repository
    singleOf(::StickerRepositoryImpl) bind StickerRepository::class
    single { SetikerApiService() }
    single { StickerApiRepository(api = get(), fileStorage = get()) }

    // ViewModels (viewModelOf = scoped to NavBackStackEntry / LocalViewModelStoreOwner)
    viewModelOf(::HomeViewModel)
    viewModelOf(::PackDetailViewModel)
    viewModelOf(::CreatePackViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::CropViewModel)
}
