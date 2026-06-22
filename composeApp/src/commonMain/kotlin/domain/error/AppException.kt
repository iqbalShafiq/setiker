package domain.error

open class AppException(
    val code: AppErrorCode,
    message: String? = null
) : Exception(message)
