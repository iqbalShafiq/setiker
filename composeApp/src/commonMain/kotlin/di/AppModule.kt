package di

import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.AuthManagerImpl
import data.auth.AuthTokenRefresher
import data.local.database.StickerDatabase
import data.local.database.SyncOperationDao
import data.preferences.UserPreferencesRepository
import data.remote.AiUsageApiRepository
import data.repository.AiQuotaRepositoryImpl
import domain.repository.AiQuotaRepository
import data.remote.CloudStickerRepository
import data.remote.ExploreApiRepository
import data.remote.LegalApiRepository
import data.remote.SetikerApiService
import data.remote.StickerApiRepository
import data.aijob.AiJobManager
import data.aijob.AiJobRunner
import data.aijob.AnimatedWorkspaceDraftHelper
import data.aijob.RoomAiJobRepository
import data.aijob.RoomWorkspaceDraftRepository
import data.repository.StickerPackDraftSaver
import data.repository.StickerRepositoryImpl
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import data.sync.NetworkMonitor
import data.sync.SyncCursorStore
import data.sync.SyncManager
import data.sync.SyncManagerImpl
import domain.repository.StickerRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import presentation.auth.LoginViewModel
import presentation.auth.ProfileViewModel
import presentation.auth.RegisterViewModel
import presentation.creator.CreatorProfileViewModel
import presentation.explore.ExploreViewModel
import presentation.notifications.NotificationsViewModel
import presentation.history.ProcessingHistoryViewModel
import presentation.home.HomeViewModel
import presentation.packdetail.PackDetailViewModel
import presentation.publicpack.PublicPackDetailViewModel
import presentation.createpack.CreatePackViewModel
import presentation.editor.EditorViewModel
import presentation.crop.CropViewModel
import presentation.videocrop.VideoCropViewModel
import presentation.videostickerpack.VideoStickerPackViewModel
import presentation.videotrim.VideoTrimViewModel
import presentation.animatededitor.AnimatedEditorViewModel
import presentation.sync.SyncViewModel
import presentation.aijobs.AiJobsViewModel
import presentation.onboarding.OnboardingViewModel
import presentation.settings.SettingsViewModel
import presentation.sharepreview.SharePreviewViewModel

expect fun platformModule(): Module

val appModule = module {
    includes(platformModule())

    // Repository
    single<StickerRepository> { StickerRepositoryImpl(get(), get(), get(), get(), get()) }
    single { SetikerApiService(authManager = get(), authTokenRefresher = get()) }
    single { StickerApiRepository(api = get(), fileStorage = get(), onDeviceImageProcessor = get()) }
    single { StickerPackDraftSaver(fileStorage = get()) }
    single { AnimatedWorkspaceDraftHelper(get(), get()) }

    // Auth
    single { AuthApiService() }
    single<AuthManager> { AuthManagerImpl(get()) }
    single { AuthTokenRefresher(authManager = get(), authApiService = get()) }
    single { CloudStickerRepository(authManager = get(), authTokenRefresher = get()) }
    single { ExploreApiRepository(authManager = get(), authTokenRefresher = get()) }
    single { LegalApiRepository(authManager = get(), authTokenRefresher = get()) }
    single { AiUsageApiRepository(authManager = get(), authTokenRefresher = get()) }
    single<AiQuotaRepository> { AiQuotaRepositoryImpl(get()) }
    single { UserPreferencesRepository(get()) }
    // NetworkMonitor is provided by platform-specific module
    single { get<StickerDatabase>().syncOperationDao() }
    single { get<StickerDatabase>().aiJobDao() }
    single { get<StickerDatabase>().workspaceDraftDao() }
    single<AiJobRepository> { RoomAiJobRepository(get()) }
    single<WorkspaceDraftRepository> { RoomWorkspaceDraftRepository(get()) }
    single { AiJobManager(get(), get(), get(), get()) }
    single { presentation.aijob.AiJobEnqueueHelper(get()) }
    single { presentation.aijob.DraftResultApplier(get(), get()) }
    single {
        AiJobRunner(
            jobRepository = get(),
            draftRepository = get(),
            apiRepository = get(),
            onDeviceImageProcessor = get(),
            fileStorage = get(),
            extractor = get(),
            gridComposer = get(),
            draftSaver = get(),
            stickerRepository = get(),
            jobManager = get(),
            animatedDraftHelper = get(),
            quotaRepository = get()
        )
    }
    single { SyncCursorStore(get()) }
    single<SyncManager> { SyncManagerImpl(get(), get(), get(), get(), get(), get(), get(), get()) }

    // ViewModels (viewModelOf = scoped to NavBackStackEntry / LocalViewModelStoreOwner)
    viewModelOf(::HomeViewModel)
    viewModelOf(::PackDetailViewModel)
    viewModelOf(::ExploreViewModel)
    viewModelOf(::CreatorProfileViewModel)
    viewModelOf(::NotificationsViewModel)
    viewModelOf(::PublicPackDetailViewModel)
    viewModelOf(::ProcessingHistoryViewModel)
    viewModelOf(::SharePreviewViewModel)
    viewModelOf(::CreatePackViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::CropViewModel)
    viewModelOf(::VideoTrimViewModel)
    viewModelOf(::VideoCropViewModel)
    viewModelOf(::VideoStickerPackViewModel)
    viewModelOf(::AnimatedEditorViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::SyncViewModel)
    viewModelOf(::AiJobsViewModel)
}
