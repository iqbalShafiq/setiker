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
import presentation.home.HomeScreenRoot
import presentation.packdetail.PackDetailScreenRoot
import presentation.createpack.CreatePackScreenRoot
import presentation.editor.EditorScreenRoot
import presentation.crop.CropScreenRoot

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onAddToWhatsApp: ((String, String) -> Unit)? = null
) {
    // Shared state for crop result (editor reads this when returning from crop)
    val cropResult = remember { mutableStateOf<String?>(null) }

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
                onEditSticker = { index, packId ->
                    navController.navigate("editor?packId=$packId&stickerIndex=$index")
                },
                onAddToWhatsApp = onAddToWhatsApp
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
            
            val croppedImagePath = cropResult.value

            EditorScreenRoot(
                stickerIndex = stickerIndex,
                packId = packId,
                croppedImagePath = croppedImagePath,
                onResultProcessed = {
                    cropResult.value = null
                },
                onBackClick = { navController.popBackStack() },
                onNavigateToCrop = { imagePath ->
                    cropResult.value = null
                    navController.navigate("crop/${PathEncoder.encode(imagePath)}")
                },
                onStickerSaved = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "crop/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("imagePath") ?: return@composable
            val imagePath = PathEncoder.decode(encodedPath)
            CropScreenRoot(
                imagePath = imagePath,
                onBackClick = { navController.popBackStack() },
                onImageCropped = { croppedPath ->
                    cropResult.value = croppedPath
                    navController.popBackStack()
                }
            )
        }
    }
}
