package data.remote

import domain.error.AppErrorCode
import domain.error.AppException

class ApiException(
    code: AppErrorCode,
    message: String? = null
) : AppException(code, message)
