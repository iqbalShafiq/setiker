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
 * Platform-agnostic multiple image picker launcher.
 */
interface MultipleImagePickerLauncher {
    fun launch()
}

/**
 * Platform-agnostic video picker launcher.
 */
interface VideoPickerLauncher {
    fun launch()
}

/**
 * Remember a platform-specific single image picker.
 *
 * @param onImagePicked Callback dengan path file lokal, atau null jika user cancel/error.
 */
@Composable
expect fun rememberImagePicker(onImagePicked: (String?) -> Unit): ImagePickerLauncher

/**
 * Remember a platform-specific multiple image picker.
 *
 * @param onImagesPicked Callback dengan list path file lokal yang dipilih.
 */
@Composable
expect fun rememberMultipleImagePicker(onImagesPicked: (List<String>) -> Unit): MultipleImagePickerLauncher

/**
 * Remember a platform-specific video **or animated GIF** picker (same pipeline as video trim).
 *
 * @param onVideoPicked Callback dengan path file lokal, atau null jika user cancel/error.
 */
@Composable
expect fun rememberVideoPicker(onVideoPicked: (String?) -> Unit): VideoPickerLauncher
