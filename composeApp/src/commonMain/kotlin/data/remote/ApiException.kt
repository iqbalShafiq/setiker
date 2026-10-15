package data.remote

import domain.error.AppErrorCode
import domain.error.AppException

class ApiException(
    code: AppErrorCode,
    message: String? = null,
    val subcode: String? = null
) : AppException(code, message)
