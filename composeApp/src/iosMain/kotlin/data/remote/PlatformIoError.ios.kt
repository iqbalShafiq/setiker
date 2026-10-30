package data.remote

import kotlinx.io.IOException

actual fun Throwable.isPlatformIoException(): Boolean = this is IOException
