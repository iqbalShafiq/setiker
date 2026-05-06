package di

import data.repository.StickerRepositoryImpl
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

    // ViewModels
    factoryOf(::HomeViewModel)
    factoryOf(::PackDetailViewModel)
    factoryOf(::CreatePackViewModel)
    factoryOf(::EditorViewModel)
    factoryOf(::CropViewModel)
    factoryOf(::BackgroundRemoverViewModel)
}
