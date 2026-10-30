package data.remote

import java.io.IOException

actual fun Throwable.isPlatformIoException(): Boolean = this is IOException
