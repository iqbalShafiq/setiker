package data.aijob

import java.io.File

actual object SourcePathValidator {
    actual fun exists(path: String): Boolean {
        if (path.isBlank()) return false
        return File(path).exists()
    }
}
