package presentation.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberImagePicker(onImagePicked: (String?) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = copyUriToInternalStorage(context, uri)
            onImagePicked(path)
        } else {
            onImagePicked(null)
        }
    }

    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                launcher.launch("image/*")
            }
        }
    }
}

@Composable
actual fun rememberMultipleImagePicker(onImagesPicked: (List<String>) -> Unit): MultipleImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val paths = uris.mapNotNull { uri ->
                copyUriToInternalStorage(context, uri)
            }
            onImagesPicked(paths)
        } else {
            onImagesPicked(emptyList())
        }
    }

    return remember {
        object : MultipleImagePickerLauncher {
            override fun launch() {
                launcher.launch("image/*")
            }
        }
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val fileName = "picked_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Opens the system document picker for any video MIME type plus image/gif.
 * PickVisualMedia with VideoOnly does not include GIF; many users keep animations as .gif files.
 */
private class OpenVideoOrGifDocument : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            // Avoid the literal "*/*" (contains "*/") — some Kotlin lexer builds choke on it here.
            type = "*".plus("/").plus("*")
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("video".plus("/").plus("*"), "image/gif")
            )
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.data
    }
}

@Composable
actual fun rememberVideoPicker(onVideoPicked: (String?) -> Unit): VideoPickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = OpenVideoOrGifDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = copyVideoUriToInternalStorage(context, uri)
            onVideoPicked(path)
        } else {
            onVideoPicked(null)
        }
    }

    return remember {
        object : VideoPickerLauncher {
            override fun launch() {
                launcher.launch(Unit)
            }
        }
    }
}

private fun copyVideoUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val ext = guessVideoExtension(context, uri) ?: "mp4"
        val fileName = "picked_video_${System.currentTimeMillis()}.$ext"
        val file = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun guessVideoExtension(context: Context, uri: Uri): String? {
    val mime = context.contentResolver.getType(uri) ?: return null
    return when {
        mime.equals("image/gif", ignoreCase = true) -> "gif"
        mime.endsWith("gif", ignoreCase = true) -> "gif"
        mime.endsWith("mp4", ignoreCase = true) -> "mp4"
        mime.endsWith("webm", ignoreCase = true) -> "webm"
        mime.endsWith("3gpp", ignoreCase = true) -> "3gp"
        mime.endsWith("quicktime", ignoreCase = true) -> "mov"
        mime.endsWith("matroska", ignoreCase = true) -> "mkv"
        else -> "mp4"
    }
}
