package data.aijob

import platform.Foundation.NSFileManager

actual object SourcePathValidator {
    actual fun exists(path: String): Boolean {
        if (path.isBlank()) return false
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}
