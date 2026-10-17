package data.remote

actual fun Throwable.isPlatformIoException(): Boolean = this is java.io.IOException
