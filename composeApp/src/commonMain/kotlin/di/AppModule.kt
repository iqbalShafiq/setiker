package di

import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.AuthManagerImpl
import data.local.database.StickerDatabase
import data.local.database.SyncOperationDao
import data.remote.CloudStickerRepository
import data.remote.SetikerApiService
import data.remote.StickerApiRepository
import data.repository.StickerRepositoryImpl
import data.storage.AnimatedStickerDraftStore
import data.sync.NetworkMonitor
import data.sync.SyncManager
import data.sync.SyncManagerImpl
import domain.repository.StickerRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import presentation.auth.LoginViewModel
import presentation.auth.ProfileViewModel
import presentation.auth.RegisterViewModel
import presentation.home.HomeViewModel
import presentation.packdetail.PackDetailViewModel
import presentation.createpack.CreatePackViewModel
import presentation.editor.EditorViewModel
import presentation.crop.CropViewModel
import presentation.videocrop.VideoCropViewModel
import presentation.videotrim.VideoTrimViewModel
import presentation.animatededitor.AnimatedEditorViewModel
import presentation.sync.SyncViewModel

expect fun platformModule(): Module

val appModule = module {
    includes(platformModule())

    // Repository
    singleOf(::StickerRepositoryImpl) bind StickerRepository::class
    single { SetikerApiService() }
    single { StickerApiRepository(api = get(), fileStorage = get()) }
    single { AnimatedStickerDraftStore() }

    // Auth
    single { AuthApiService() }
    single<AuthManager> { AuthManagerImpl(get()) }
    single { CloudStickerRepository(authManager = get()) }
    // NetworkMonitor is provided by platform-specific module
    single { get<StickerDatabase>().syncOperationDao() }
    single<SyncManager> { SyncManagerImpl(get(), get(), get(), get()) }

    // ViewModels (viewModelOf = scoped to NavBackStackEntry / LocalViewModelStoreOwner)
    viewModelOf(::HomeViewModel)
    viewModelOf(::PackDetailViewModel)
    viewModelOf(::CreatePackViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::CropViewModel)
    viewModelOf(::VideoTrimViewModel)
    viewModelOf(::VideoCropViewModel)
    viewModelOf(::AnimatedEditorViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SyncViewModel)
}
