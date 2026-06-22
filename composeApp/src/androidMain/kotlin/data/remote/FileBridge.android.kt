package data.remote

import java.io.File

actual fun readFileBytes(path: String): ByteArray = File(path).readBytes()
