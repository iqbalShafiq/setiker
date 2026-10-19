package data.remote

actual fun Throwable.isPlatformIoException(): Boolean = this is kotlinx.io.IOException
