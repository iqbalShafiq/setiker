package presentation.components

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic image picker launcher.
 * Implementasi actual ada di androidMain dan iosMain.
 */
interface ImagePickerLauncher {
    fun launch()
}

/**
 * Remember a platform-specific image picker.
 *
 * @param onImagePicked Callback dengan path file lokal, atau null jika user cancel/error.
 */
@Composable
expect fun rememberImagePicker(onImagePicked: (String?) -> Unit): ImagePickerLauncher
