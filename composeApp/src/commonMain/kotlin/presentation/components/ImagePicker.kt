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

/**
 * Remember a picker untuk tombol "Add" di sticker pack editor. Menerima static image
 * maupun animated GIF dalam satu picker. Static image diarahkan ke jalur crop biasa,
 * sementara GIF diarahkan ke jalur animated (VideoTrim -> VideoCrop -> AnimatedEditor),
 * konsisten dengan tombol movie di bottom action bar.
 *
 * @param onPicked Callback dengan path file lokal dan flag `isAnimated`.
 *                 `isAnimated == true` saat hasil pilihan berupa GIF.
 *                 `path == null` saat user batal atau gagal copy.
 */
@Composable
expect fun rememberStickerImagePicker(
    onPicked: (path: String?, isAnimated: Boolean) -> Unit
): ImagePickerLauncher
