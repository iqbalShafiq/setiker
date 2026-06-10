package presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navDeepLink
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import domain.model.AnimatedStickerSpec
import domain.model.AuthState
import data.auth.AuthManager
import data.preferences.UserPreferencesRepository
import data.sync.NetworkMonitor
import presentation.animatededitor.AnimatedEditorEffect
import presentation.animatededitor.AnimatedEditorScreenRoot
import presentation.auth.LoginScreenRoot
import presentation.auth.LoginViewModel
import presentation.auth.ProfileScreenRoot
import presentation.auth.ProfileViewModel
import presentation.auth.RegisterScreenRoot
import presentation.auth.RegisterViewModel
import presentation.createpack.CreatePackScreenRoot
import presentation.createpack.DraftSticker
import presentation.crop.CropScreenRoot
import presentation.editor.EditorScreenRoot
import presentation.creator.CreatorProfileScreenRoot
import presentation.explore.ExploreScreenRoot
import presentation.notifications.NotificationsScreenRoot
import presentation.aijobs.AiJobsScreenRoot
import presentation.history.ProcessingHistoryScreenRoot
import presentation.home.HomeScreenRoot
import presentation.packdetail.PackDetailScreenRoot
import presentation.publicpack.PublicPackDetailScreenRoot
import presentation.sync.SyncScreenRoot
import presentation.sync.SyncViewModel
import presentation.onboarding.OnboardingScreenRoot
import presentation.settings.SettingsScreenRoot
import presentation.sharepreview.SharePreviewScreenRoot
import presentation.videocrop.VideoCropScreenRoot
import presentation.videostickerpack.VideoStickerPackScreenRoot
import presentation.videotrim.VideoTrimScreenRoot
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import presentation.aijob.WorkspaceDraftNavigation
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private object CropRecipient {
    const val Editor = "editor"
    const val CreatePackSticker = "create_pack_sticker"
    const val CreatePackTray = "create_pack_tray"
    const val PackDetailImport = "pack_detail_import"
}

private const val VIDEO_STICKER_PACK_ROUTE = "videoStickerPack/{videoPath}"

private fun Screen.VideoStickerPack.toRoute(): String =
    "videoStickerPack/${PathEncoder.encode(videoPath)}"

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onAddToWhatsApp: ((String, String) -> Unit)? = null,
    notificationDeepLink: NotificationDeepLink? = null,
    notificationDeepLinkVersion: Int = 0
) {
    val authManager: AuthManager = koinInject()
    val userPreferencesRepository: UserPreferencesRepository = koinInject()
    val networkMonitor: NetworkMonitor = koinInject()
    val onboardingCompleted by userPreferencesRepository.hasCompletedOnboarding.collectAsState(initial = true)
    val isOnline by networkMonitor.isOnline.collectAsState()
    val workspaceDraftRepository: WorkspaceDraftRepository = koinInject()
    val aiJobRepository: AiJobRepository = koinInject()
    val authState by authManager.authState.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val loginGuardRoutes = setOf("home", "profile", "sync", "history", "aiJobs")

    val navigationTransitions = remember { NavigationTransitionController() }
    fun navBottomUp(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        navigationTransitions.navigateBottomUp(navController, route, builder)
    }
    fun navTopDown(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        navigationTransitions.navigateTopDown(navController, route, builder)
    }

    DisposableEffect(navController, navigationTransitions) {
        val unbind = navigationTransitions.bindTo(navController)
        onDispose { unbind() }
    }

    LaunchedEffect(Unit) {
        networkMonitor.startMonitoring()
    }

    LaunchedEffect(onboardingCompleted, currentRoute) {
        if (!onboardingCompleted && currentRoute == "home") {
            navBottomUp("onboarding") {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(authState, currentRoute) {
        if (authState == AuthState.UNAUTHENTICATED) {
            if (currentRoute in loginGuardRoutes) {
                navBottomUp("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(notificationDeepLinkVersion, notificationDeepLink) {
        val link = notificationDeepLink ?: return@LaunchedEffect
        if (link.openAiJobsOnly) {
            navBottomUp("aiJobs") {
                launchSingleTop = true
                popUpTo("home") { inclusive = false }
            }
            return@LaunchedEffect
        }
        if (link.draftId.isNullOrBlank() && link.jobId.isNullOrBlank()) return@LaunchedEffect
        val target = WorkspaceDraftNavigation.resolveNotificationTarget(
            draftRepository = workspaceDraftRepository,
            jobRepository = aiJobRepository,
            draftId = link.draftId,
            jobId = link.jobId
        )
        navigateToNotificationTarget(navigationTransitions, navController, target)
    }

    val editorCropResult = remember { mutableStateOf<String?>(null) }
    val createPackStickerCrop = remember { mutableStateOf<String?>(null) }
    val createPackTrayCrop = remember { mutableStateOf<String?>(null) }
    val packDetailImportCrop = remember { mutableStateOf<String?>(null) }
    val pendingAnimatedDraft = remember { mutableStateOf<DraftSticker?>(null) }

    // Hoist reads: when state is written only inside inactive composables, Compose may skip
    // invalidating the hierarchy; reading here keeps AppNavigation subscribed while Crop shows.
    val editorCropDeliveredPath = editorCropResult.value
    val createStickerCropDeliveredPath = createPackStickerCrop.value
    val createTrayCropDeliveredPath = createPackTrayCrop.value
    val packDetailImportDeliveredPath = packDetailImportCrop.value
    val pendingAnimatedDraftDelivered = pendingAnimatedDraft.value

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { navigationTransitions.enterTransition(this) },
        exitTransition = { navigationTransitions.exitTransition(this) },
        popEnterTransition = { navigationTransitions.popEnterTransition(this) },
        popExitTransition = { navigationTransitions.popExitTransition(this) }
    ) {
        composable("onboarding") {
            OnboardingScreenRoot(
                onFinished = {
                    navBottomUp("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreenRoot(
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navBottomUp("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onShowOnboarding = {
                    navBottomUp("onboarding") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("home") {
            HomeScreenRoot(
                onPackClick = { packId ->
                    navBottomUp("packDetail/$packId")
                },
                onCreatePackClick = {
                    navBottomUp("createPack")
                },
                onExploreClick = {
                    navBottomUp("explore")
                },
                onProfileClick = {
                    navBottomUp("profile")
                },
                onSyncClick = {
                    navBottomUp("sync")
                },
                onLoginClick = {
                    navBottomUp("login")
                },
                onVideoStickerPackClick = { videoPath ->
                    navBottomUp(Screen.VideoStickerPack(videoPath).toRoute())
                },
                onAiJobsClick = { navBottomUp("aiJobs") },
                onTopBarAiJobsClick = { navTopDown("aiJobs") },
                showOfflineBanner = !isOnline
            )
        }

        composable("aiJobs") {
            AiJobsScreenRoot(
                onBackClick = { navController.popBackStack() },
                onOpenDraft = { draftId, originRoute ->
                    navigateToWorkspaceDraft(navigationTransitions, navController, draftId, originRoute)
                },
                onOpenPack = { packId -> navBottomUp("packDetail/$packId") }
            )
        }

        composable(
            route = VIDEO_STICKER_PACK_ROUTE,
            arguments = listOf(navArgument("videoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("videoPath") ?: return@composable
            val route = Screen.VideoStickerPack(PathEncoder.decode(encodedPath))
            VideoStickerPackScreenRoot(
                videoPath = route.videoPath,
                onBackClick = { navController.popBackStack() },
                onNavigateToPackDetail = { packId ->
                    navBottomUp("packDetail/$packId") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable("login") {
             val viewModel: LoginViewModel = koinViewModel()
             LoginScreenRoot(
                 viewModel = viewModel,
                 onNavigateToHome = {
                     navBottomUp("home") {
                         popUpTo("home") { inclusive = true }
                     }
                 },
                 onNavigateToRegister = {
                     navBottomUp("register") {
                         popUpTo("login") { inclusive = true }
                     }
                 }
             )
         }
 
        composable("register") {
               val viewModel: RegisterViewModel = koinViewModel()
               RegisterScreenRoot(
                   viewModel = viewModel,
                   onNavigateToHome = {
                       navBottomUp("home") {
                           popUpTo("home") { inclusive = true }
                       }
                   },
                   onNavigateToLogin = {
                       navBottomUp("login") {
                           popUpTo("register") { inclusive = true }
                       }
                   }
               )
           }
  
        composable("profile") {
            val viewModel: ProfileViewModel = koinViewModel()
            ProfileScreenRoot(
                viewModel = viewModel,
                onLogout = {
                    navBottomUp("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onBackClick = { navController.navigateUp() },
                onSettingsClick = { navBottomUp("settings") },
                onNavigateHome = {
                    navBottomUp("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateExplore = {
                    navBottomUp("explore")
                },
                onNavigateHistory = {
                    if (authState == AuthState.AUTHENTICATED) {
                        navBottomUp("history")
                    } else {
                        navBottomUp("login")
                    }
                },
                onNavigateAiJobs = {
                    if (authState == AuthState.AUTHENTICATED) {
                        navBottomUp("aiJobs")
                    } else {
                        navBottomUp("login")
                    }
                },
                onNavigateNotifications = {
                    if (authState == AuthState.AUTHENTICATED) {
                        navTopDown("notifications")
                    } else {
                        navBottomUp("login")
                    }
                }
            )
        }

        composable("explore") {
            ExploreScreenRoot(
                onBackClick = { navController.popBackStack() },
                onPackClick = { packId -> navBottomUp("publicPack/$packId") },
                onCreatorClick = { userId -> navBottomUp("creator/$userId") },
                onHistoryClick = {
                    if (authState == AuthState.AUTHENTICATED) navBottomUp("history")
                    else navBottomUp("login")
                },
                onLoginClick = { navBottomUp("login") }
            )
        }

        composable(
            route = "creator/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            CreatorProfileScreenRoot(
                userId = userId,
                onBackClick = { navController.popBackStack() },
                onPackClick = { packId -> navBottomUp("publicPack/$packId") }
            )
        }

        composable("notifications") {
            NotificationsScreenRoot(
                onBackClick = { navController.popBackStack() },
                onNavigateToPublicPack = { packId -> navBottomUp("publicPack/$packId") },
                onNavigateToCreator = { userId -> navBottomUp("creator/$userId") }
            )
        }

        composable(
            route = "publicPack/{packId}",
            arguments = listOf(navArgument("packId") { type = NavType.StringType })
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId") ?: return@composable
            PublicPackDetailScreenRoot(
                packId = packId,
                onBackClick = { navController.popBackStack() },
                onNavigateToLocalPack = { localPackId ->
                    navBottomUp("packDetail/$localPackId") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateToLogin = { navBottomUp("login") },
                onNavigateToCreator = { userId -> navBottomUp("creator/$userId") }
            )
        }

        composable("history") {
            ProcessingHistoryScreenRoot(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "sharePreview/pack/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "http://13.251.98.162/api/v1/share/pack/{token}" },
                navDeepLink { uriPattern = "setiker://share/pack/{token}" }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            SharePreviewScreenRoot(
                kind = "pack",
                token = token,
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = { navBottomUp("login") },
                onNavigateToLocalPack = { localPackId ->
                    navBottomUp("packDetail/$localPackId") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "sharePreview/sticker/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "http://13.251.98.162/api/v1/share/sticker/{token}" },
                navDeepLink { uriPattern = "setiker://share/sticker/{token}" }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            SharePreviewScreenRoot(
                kind = "sticker",
                token = token,
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = { navBottomUp("login") },
                onNavigateToLocalPack = { localPackId ->
                    navBottomUp("packDetail/$localPackId") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable("sync") {
            val viewModel: SyncViewModel = koinViewModel()
            SyncScreenRoot(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "packDetail/{packId}",
            arguments = listOf(navArgument("packId") { type = NavType.StringType })
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId") ?: return@composable
            PackDetailScreenRoot(
                packId = packId,
                onBackClick = { navController.popBackStack() },
                onEditPack = { navBottomUp("createPack?packId=$it") },
                onAddSticker = { navBottomUp("editor?packId=$it") },
                onEditSticker = { index, pId ->
                    navBottomUp("editor?packId=$pId&stickerIndex=$index")
                },
                onAddToWhatsApp = onAddToWhatsApp,
                croppedStickerImportPath = packDetailImportDeliveredPath,
                onStickerImportCropConsumed = { packDetailImportCrop.value = null },
                onNavigateToCropForStickerImport = { path ->
                    packDetailImportCrop.value = null
                    navBottomUp(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.PackDetailImport}"
                    )
                },
                onNavigateToPack = { newPackId ->
                    navBottomUp("packDetail/$newPackId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToPublicPack = { cloudId ->
                    navBottomUp("publicPack/$cloudId")
                }
            )
        }

        composable("createPack") {
            CreatePackScreenRoot(
                packId = null,
                workspaceDraftId = null,
                onBackClick = { navController.popBackStack() },
                onPackSaved = { savedPackId ->
                    navBottomUp("packDetail/$savedPackId") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                croppedStickerGalleryPath = createStickerCropDeliveredPath,
                croppedTrayGalleryPath = createTrayCropDeliveredPath,
                pendingAnimatedDraft = pendingAnimatedDraftDelivered,
                onStickerGalleryCropConsumed = { createPackStickerCrop.value = null },
                onTrayGalleryCropConsumed = { createPackTrayCrop.value = null },
                onAnimatedDraftConsumed = { pendingAnimatedDraft.value = null },
                onNavigateToCropSticker = { path ->
                    createPackStickerCrop.value = null
                    navBottomUp(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.CreatePackSticker}"
                    )
                },
                onNavigateToCropTray = { path ->
                    createPackTrayCrop.value = null
                    navBottomUp(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.CreatePackTray}"
                    )
                },
                onNavigateToVideoTrim = { videoPath ->
                    pendingAnimatedDraft.value = null
                    navBottomUp(
                        "videoTrim/${PathEncoder.encode(videoPath)}?packId="
                    )
                },
                onNavigateToPublicPack = { cloudId ->
                    navBottomUp("publicPack/$cloudId")
                }
            )
        }

        composable(
            route = "createPack?packId={packId}&workspaceDraftId={workspaceDraftId}",
            arguments = listOf(
                navArgument("packId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("workspaceDraftId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId")
            val workspaceDraftId = backStackEntry.arguments?.getString("workspaceDraftId")
            CreatePackScreenRoot(
                packId = packId,
                workspaceDraftId = workspaceDraftId,
                onBackClick = { navController.popBackStack() },
                onPackSaved = { savedPackId ->
                    navBottomUp("packDetail/$savedPackId") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                croppedStickerGalleryPath = createStickerCropDeliveredPath,
                croppedTrayGalleryPath = createTrayCropDeliveredPath,
                pendingAnimatedDraft = pendingAnimatedDraftDelivered,
                onStickerGalleryCropConsumed = { createPackStickerCrop.value = null },
                onTrayGalleryCropConsumed = { createPackTrayCrop.value = null },
                onAnimatedDraftConsumed = { pendingAnimatedDraft.value = null },
                onNavigateToCropSticker = { path ->
                    createPackStickerCrop.value = null
                    navBottomUp(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.CreatePackSticker}"
                    )
                },
                onNavigateToCropTray = { path ->
                    createPackTrayCrop.value = null
                    navBottomUp(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.CreatePackTray}"
                    )
                },
                onNavigateToVideoTrim = { videoPath ->
                    pendingAnimatedDraft.value = null
                    val effectivePackId = packId.orEmpty()
                    navBottomUp(
                        "videoTrim/${PathEncoder.encode(videoPath)}?packId=${PathEncoder.encode(effectivePackId)}"
                    )
                },
                onNavigateToPublicPack = { cloudId ->
                    navBottomUp("publicPack/$cloudId")
                }
            )
        }

        composable(
            route = "editor?packId={packId}&stickerIndex={stickerIndex}",
            arguments = listOf(
                navArgument("packId") { type = NavType.StringType },
                navArgument("stickerIndex") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId") ?: return@composable
            val stickerIndex = backStackEntry.arguments?.getInt("stickerIndex")?.takeIf { it >= 0 }

            val croppedImagePath = editorCropDeliveredPath

            EditorScreenRoot(
                stickerIndex = stickerIndex,
                packId = packId,
                croppedImagePath = croppedImagePath,
                onResultProcessed = {
                    editorCropResult.value = null
                },
                onBackClick = { navController.popBackStack() },
                onNavigateToCrop = { imagePath ->
                    editorCropResult.value = null
                    navBottomUp(
                        "crop/${PathEncoder.encode(imagePath)}/${CropRecipient.Editor}"
                    )
                },
                onStickerSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = "crop/{imagePath}/{recipient}",
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType },
                navArgument("recipient") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("imagePath") ?: return@composable
            val recipient = backStackEntry.arguments?.getString("recipient")
                ?: CropRecipient.Editor
            val imagePath = PathEncoder.decode(encodedPath)
            CropScreenRoot(
                imagePath = imagePath,
                onBackClick = { navController.popBackStack() },
                onImageCropped = { croppedPath ->
                    when (recipient) {
                        CropRecipient.Editor -> editorCropResult.value = croppedPath
                        CropRecipient.CreatePackSticker -> createPackStickerCrop.value = croppedPath
                        CropRecipient.CreatePackTray -> createPackTrayCrop.value = croppedPath
                        CropRecipient.PackDetailImport -> packDetailImportCrop.value = croppedPath
                        else -> Unit
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "videoTrim/{videoPath}?packId={packId}",
            arguments = listOf(
                navArgument("videoPath") { type = NavType.StringType },
                navArgument("packId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("videoPath") ?: return@composable
            val videoPath = PathEncoder.decode(encodedPath)
            val encodedPackId = backStackEntry.arguments?.getString("packId").orEmpty()
            val packId = if (encodedPackId.isBlank()) "" else PathEncoder.decode(encodedPackId)
            VideoTrimScreenRoot(
                videoPath = videoPath,
                onBackClick = { navController.popBackStack() },
                onNavigateToVideoCrop = { vp, spec ->
                    navBottomUp(
                        "videoCrop/${PathEncoder.encode(vp)}" +
                            "?packId=${PathEncoder.encode(packId)}" +
                            "&trimStart=${spec.trimStartMs}" +
                            "&trimEnd=${spec.trimEndMs}" +
                            "&fps=${spec.fps}" +
                            "&speed=${spec.speed}"
                    )
                }
            )
        }

        composable(
            route = "videoCrop/{videoPath}?packId={packId}&trimStart={trimStart}&trimEnd={trimEnd}&fps={fps}&speed={speed}",
            arguments = listOf(
                navArgument("videoPath") { type = NavType.StringType },
                navArgument("packId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("trimStart") { type = NavType.LongType },
                navArgument("trimEnd") { type = NavType.LongType },
                navArgument("fps") { type = NavType.IntType },
                navArgument("speed") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("videoPath") ?: return@composable
            val videoPath = PathEncoder.decode(encodedPath)
            val encodedPackId = backStackEntry.arguments?.getString("packId").orEmpty()
            val packIdArg = if (encodedPackId.isBlank()) "" else PathEncoder.decode(encodedPackId)
            val spec = AnimatedStickerSpec(
                trimStartMs = backStackEntry.arguments?.getLong("trimStart") ?: 0L,
                trimEndMs = backStackEntry.arguments?.getLong("trimEnd") ?: 0L,
                fps = backStackEntry.arguments?.getInt("fps") ?: AnimatedStickerSpec.DEFAULT_FPS,
                speed = backStackEntry.arguments?.getFloat("speed") ?: 1f
            )
            VideoCropScreenRoot(
                videoPath = videoPath,
                spec = spec,
                packId = packIdArg,
                onBackClick = { navController.popBackStack() },
                onNavigateToAnimatedEditor = { draftId ->
                    navBottomUp(
                        "animatedEditor/$draftId?packId=${PathEncoder.encode(packIdArg)}"
                    ) {
                        popUpTo("createPack?packId=${packIdArg.takeIf { it.isNotBlank() } ?: "{packId}"}") {
                            inclusive = false
                        }
                    }
                }
            )
        }

        composable(
            route = "animatedEditor/{draftId}?packId={packId}",
            arguments = listOf(
                navArgument("draftId") { type = NavType.StringType },
                navArgument("packId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val draftId = backStackEntry.arguments?.getString("draftId") ?: return@composable
            val encodedPackId = backStackEntry.arguments?.getString("packId").orEmpty()
            val packId = if (encodedPackId.isBlank()) "" else PathEncoder.decode(encodedPackId)
            AnimatedEditorScreenRoot(
                draftId = draftId,
                packId = packId,
                onBackClick = { navController.popBackStack() },
                onAnimatedDraftReady = { ready: AnimatedEditorEffect.AnimatedDraftReady ->
                    pendingAnimatedDraft.value = DraftSticker(
                        imagePath = ready.imagePath,
                        decorations = ready.baseDecorations,
                        isAnimated = true,
                        sourceVideoFile = ready.sourceVideoFile,
                        frameDecorations = ready.frameDecorations
                    )
                    val createPackRoute = if (packId.isBlank()) "createPack" else "createPack?packId=$packId"
                    navController.popBackStack(createPackRoute, inclusive = false)
                }
            )
        }
    }
}

private fun navigateToWorkspaceDraft(
    navigationTransitions: NavigationTransitionController,
    navController: NavHostController,
    draftId: String,
    originRoute: String?
) {
    val route = WorkspaceDraftNavigation.routeForDraft(draftId, originRoute)
    navigationTransitions.navigateBottomUp(navController, route) {
        launchSingleTop = true
    }
}

private fun navigateToNotificationTarget(
    navigationTransitions: NavigationTransitionController,
    navController: NavHostController,
    target: WorkspaceDraftNavigation.Target
) {
    if (target.navigateToAiJobsFirst) {
        navigationTransitions.navigateBottomUp(navController, "aiJobs") {
            launchSingleTop = true
            popUpTo("home") { inclusive = false }
        }
    }
    navigationTransitions.navigateBottomUp(navController, target.route) {
        launchSingleTop = true
        popUpTo("home") { inclusive = false }
    }
}
