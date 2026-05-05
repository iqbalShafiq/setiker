package di

import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import presentation.home.HomeViewModel
import presentation.packdetail.PackDetailViewModel
import presentation.createpack.CreatePackViewModel
import presentation.editor.EditorViewModel
import presentation.crop.CropViewModel
import presentation.backgroundremover.BackgroundRemoverViewModel

expect fun platformModule(): Module

val appModule = module {
    // ViewModels
    factoryOf(::HomeViewModel)
    factoryOf(::PackDetailViewModel)
    factoryOf(::CreatePackViewModel)
    factoryOf(::EditorViewModel)
    factoryOf(::CropViewModel)
    factoryOf(::BackgroundRemoverViewModel)
}
