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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Guarantees unique picked-file names even when several files are copied in the same millisecond. */
private val pickedFileSequence = AtomicLong()

private fun nextPickedFileName(prefix: String, extension: String): String =
    "${prefix}_${System.currentTimeMillis()}_${pickedFileSequence.incrementAndGet()}.$extension"

@Composable
actual fun rememberImagePicker(onImagePicked: (String?) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            onImagePicked(null)
        } else {
            // Copying a picked file can be tens of megabytes; keep it off the main thread.
            scope.launch {
                onImagePicked(withContext(Dispatchers.IO) { copyUriToInternalStorage(context, uri) })
            }
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
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) {
            onImagesPicked(emptyList())
        } else {
            scope.launch {
                val paths = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri -> copyUriToInternalStorage(context, uri) }
                }
                onImagesPicked(paths)
            }
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
        val file = File(context.filesDir, nextPickedFileName("picked", "jpg"))
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
actual fun rememberStickerImagePicker(
    onPicked: (path: String?, isAnimated: Boolean) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            onPicked(null, false)
            return@rememberLauncherForActivityResult
        }
        val isGif = context.contentResolver.getType(uri)?.equals("image/gif", ignoreCase = true) == true
        val extension = if (isGif) "gif" else "jpg"
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                copyPickedUriToInternalStorage(context, uri, extension)
            }
            onPicked(path, isGif && path != null)
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

private fun copyPickedUriToInternalStorage(
    context: Context,
    uri: Uri,
    extension: String
): String? {
    return try {
        val file = File(context.filesDir, nextPickedFileName("picked", extension))
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

@Composable
actual fun rememberVideoPicker(onVideoPicked: (String?) -> Unit): VideoPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = OpenVideoOrGifDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            onVideoPicked(null)
        } else {
            // Videos can be large; copy them off the main thread.
            scope.launch {
                onVideoPicked(withContext(Dispatchers.IO) { copyVideoUriToInternalStorage(context, uri) })
            }
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
        val extension = guessVideoExtension(context, uri) ?: "mp4"
        val file = File(context.filesDir, nextPickedFileName("picked_video", extension))
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
