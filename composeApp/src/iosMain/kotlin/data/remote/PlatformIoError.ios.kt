package data.remote

actual fun Throwable.isPlatformIoException(): Boolean = this is io.ktor.utils.io.errors.IOException
