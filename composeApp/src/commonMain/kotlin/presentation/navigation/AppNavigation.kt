package presentation.navigation

 import androidx.compose.animation.AnimatedContentTransitionScope
 import androidx.compose.animation.core.tween
 import androidx.compose.animation.fadeIn
 import androidx.compose.animation.fadeOut
 import androidx.compose.animation.slideInHorizontally
 import androidx.compose.animation.slideInVertically
 import androidx.compose.animation.slideOutHorizontally
 import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
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
import presentation.explore.ExploreScreenRoot
import presentation.history.ProcessingHistoryScreenRoot
import presentation.home.HomeScreenRoot
import presentation.packdetail.PackDetailScreenRoot
import presentation.publicpack.PublicPackDetailScreenRoot
import presentation.sync.SyncScreenRoot
import presentation.sync.SyncViewModel
import presentation.sharepreview.SharePreviewScreenRoot
import presentation.videocrop.VideoCropScreenRoot
import presentation.videotrim.VideoTrimScreenRoot
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private object CropRecipient {
    const val Editor = "editor"
    const val CreatePackSticker = "create_pack_sticker"
    const val CreatePackTray = "create_pack_tray"
    const val PackDetailImport = "pack_detail_import"
}

private const val ANIMATION_DURATION = 300

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onAddToWhatsApp: ((String, String) -> Unit)? = null
) {
    val authManager: AuthManager = koinInject()
    val authState by authManager.authState.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val loginGuardRoutes = setOf("home", "profile", "sync", "history")

    LaunchedEffect(authState, currentRoute) {
        if (authState == AuthState.UNAUTHENTICATED) {
            if (currentRoute in loginGuardRoutes) {
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
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
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreenRoot(
                onPackClick = { packId ->
                    navController.navigate("packDetail/$packId")
                },
                onCreatePackClick = {
                    navController.navigate("createPack")
                },
                onExploreClick = {
                    navController.navigate("explore")
                },
                onProfileClick = {
                    navController.navigate("profile")
                },
                onSyncClick = {
                    navController.navigate("sync")
                },
                onLoginClick = {
                    navController.navigate("login")
                }
            )
        }

         composable("login",
              enterTransition = {
                  when (initialState.destination.route) {
                      "home" -> slideInVertically(
                          initialOffsetY = { it },
                          animationSpec = tween(ANIMATION_DURATION)
                      ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
                      "register" -> slideInHorizontally(
                          initialOffsetX = { -it },
                          animationSpec = tween(ANIMATION_DURATION)
                      ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
                      else -> slideInHorizontally(
                          initialOffsetX = { it },
                          animationSpec = tween(ANIMATION_DURATION)
                      ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
                  }
              },
             exitTransition = {
                 when (targetState.destination.route) {
                     "register" -> slideOutHorizontally(
                         targetOffsetX = { -it },
                         animationSpec = tween(ANIMATION_DURATION)
                     ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
                     else -> slideOutVertically(
                         targetOffsetY = { it },
                         animationSpec = tween(ANIMATION_DURATION)
                     ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
                 }
             },
             popEnterTransition = {
                 when (initialState.destination.route) {
                     "register" -> slideInHorizontally(
                         initialOffsetX = { -it },
                         animationSpec = tween(ANIMATION_DURATION)
                     ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
                     else -> slideInVertically(
                         initialOffsetY = { it },
                         animationSpec = tween(ANIMATION_DURATION)
                     ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
                 }
             },
              popExitTransition = {
                  when (targetState.destination.route) {
                      "register" -> slideOutHorizontally(
                          targetOffsetX = { it },
                          animationSpec = tween(ANIMATION_DURATION)
                      ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
                      else -> slideOutVertically(
                          targetOffsetY = { it },
                          animationSpec = tween(ANIMATION_DURATION)
                      ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
                  }
              }
          ) {
             val viewModel: LoginViewModel = koinViewModel()
             LoginScreenRoot(
                 viewModel = viewModel,
                 onNavigateToHome = {
                     navController.navigate("home") {
                         popUpTo("home") { inclusive = true }
                     }
                 },
                 onNavigateToRegister = {
                     navController.navigate("register") {
                         popUpTo("login") { inclusive = true }
                     }
                 }
             )
         }
 
          composable("register",
              enterTransition = {
                  slideInHorizontally(
                      initialOffsetX = { it },
                      animationSpec = tween(ANIMATION_DURATION)
                  ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
              },
              exitTransition = {
                  when (targetState.destination.route) {
                      "login" -> slideOutHorizontally(
                          targetOffsetX = { it },
                          animationSpec = tween(ANIMATION_DURATION)
                      ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
                      else -> slideOutHorizontally(
                          targetOffsetX = { -it },
                          animationSpec = tween(ANIMATION_DURATION)
                      ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
                  }
              },
              popEnterTransition = {
                  slideInHorizontally(
                      initialOffsetX = { -it },
                      animationSpec = tween(ANIMATION_DURATION)
                  ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
              },
               popExitTransition = {
                   slideOutHorizontally(
                       targetOffsetX = { it },
                       animationSpec = tween(ANIMATION_DURATION)
                   ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
               }
           ) {
               val viewModel: RegisterViewModel = koinViewModel()
               RegisterScreenRoot(
                   viewModel = viewModel,
                   onNavigateToHome = {
                       navController.navigate("home") {
                           popUpTo("home") { inclusive = true }
                       }
                   },
                   onNavigateToLogin = {
                       navController.navigate("login") {
                           popUpTo("register") { inclusive = true }
                       }
                   }
               )
           }
  
          composable("profile",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
            }
        ) {
            val viewModel: ProfileViewModel = koinViewModel()
            ProfileScreenRoot(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onBackClick = { navController.navigateUp() },
                onSettingsClick = {},
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateExplore = {
                    navController.navigate("explore")
                },
                onNavigateHistory = {
                    if (authState == AuthState.AUTHENTICATED) {
                        navController.navigate("history")
                    } else {
                        navController.navigate("login")
                    }
                }
            )
        }

        composable("explore") {
            ExploreScreenRoot(
                onBackClick = { navController.popBackStack() },
                onPackClick = { packId -> navController.navigate("publicPack/$packId") },
                onHistoryClick = {
                    if (authState == AuthState.AUTHENTICATED) navController.navigate("history")
                    else navController.navigate("login")
                }
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
                    navController.navigate("packDetail/$localPackId") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateToLogin = { navController.navigate("login") }
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
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToLocalPack = { localPackId ->
                    navController.navigate("packDetail/$localPackId") {
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
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToLocalPack = { localPackId ->
                    navController.navigate("packDetail/$localPackId") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable("sync",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
            }
        ) {
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
                onEditPack = { navController.navigate("createPack?packId=$it") },
                onAddSticker = { navController.navigate("editor?packId=$it") },
                onEditSticker = { index, pId ->
                    navController.navigate("editor?packId=$pId&stickerIndex=$index")
                },
                onAddToWhatsApp = onAddToWhatsApp,
                croppedStickerImportPath = packDetailImportDeliveredPath,
                onStickerImportCropConsumed = { packDetailImportCrop.value = null },
                onNavigateToCropForStickerImport = { path ->
                    packDetailImportCrop.value = null
                    navController.navigate(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.PackDetailImport}"
                    )
                }
            )
        }

        composable(
            route = "createPack?packId={packId}",
            arguments = listOf(
                navArgument("packId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId")
            CreatePackScreenRoot(
                packId = packId,
                onBackClick = { navController.popBackStack() },
                onPackSaved = { savedPackId ->
                    navController.navigate("packDetail/$savedPackId") {
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
                    navController.navigate(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.CreatePackSticker}"
                    )
                },
                onNavigateToCropTray = { path ->
                    createPackTrayCrop.value = null
                    navController.navigate(
                        "crop/${PathEncoder.encode(path)}/${CropRecipient.CreatePackTray}"
                    )
                },
                onNavigateToVideoTrim = { videoPath ->
                    pendingAnimatedDraft.value = null
                    val effectivePackId = packId.orEmpty()
                    navController.navigate(
                        "videoTrim/${PathEncoder.encode(videoPath)}?packId=${PathEncoder.encode(effectivePackId)}"
                    )
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
                    navController.navigate(
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
                    navController.navigate(
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
                    navController.navigate(
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
