package presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import presentation.createpack.CreatePackScreenRoot
import presentation.crop.CropScreenRoot
import presentation.editor.EditorScreenRoot
import presentation.home.HomeScreenRoot
import presentation.packdetail.PackDetailScreenRoot

private object CropRecipient {
    const val Editor = "editor"
    const val CreatePackSticker = "create_pack_sticker"
    const val CreatePackTray = "create_pack_tray"
    const val PackDetailImport = "pack_detail_import"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onAddToWhatsApp: ((String, String) -> Unit)? = null
) {
    val editorCropResult = remember { mutableStateOf<String?>(null) }
    val createPackStickerCrop = remember { mutableStateOf<String?>(null) }
    val createPackTrayCrop = remember { mutableStateOf<String?>(null) }
    val packDetailImportCrop = remember { mutableStateOf<String?>(null) }

    // Hoist reads: when state is written only inside inactive composables, Compose may skip
    // invalidating the hierarchy; reading here keeps AppNavigation subscribed while Crop shows.
    val editorCropDeliveredPath = editorCropResult.value
    val createStickerCropDeliveredPath = createPackStickerCrop.value
    val createTrayCropDeliveredPath = createPackTrayCrop.value
    val packDetailImportDeliveredPath = packDetailImportCrop.value

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
                }
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
                onStickerGalleryCropConsumed = { createPackStickerCrop.value = null },
                onTrayGalleryCropConsumed = { createPackTrayCrop.value = null },
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
    }
}
