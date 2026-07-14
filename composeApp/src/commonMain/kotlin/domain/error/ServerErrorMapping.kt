package domain.error

fun serverSubcodeToAppErrorCode(subcode: String?): AppErrorCode? {
    return when (subcode) {
        "INVALID_CREDENTIALS" -> AppErrorCode.AuthLoginFailed
        "ACCOUNT_INACTIVE" -> AppErrorCode.AuthAccountDeactivated
        "TOKEN_EXPIRED" -> AppErrorCode.AuthRefreshFailed
        "TOKEN_INVALID" -> AppErrorCode.AuthRefreshFailed
        "REFRESH_TOKEN_MISSING" -> AppErrorCode.AuthRefreshFailed
        "REFRESH_TOKEN_INVALID" -> AppErrorCode.AuthRefreshFailed
        "REFRESH_TOKEN_NOT_FOUND" -> AppErrorCode.AuthRefreshFailed
        "REFRESH_TOKEN_EXPIRED" -> AppErrorCode.AuthRefreshFailed
        "EMAIL_ALREADY_IN_USE" -> AppErrorCode.AuthRegisterFailed
        "USERNAME_ALREADY_IN_USE" -> AppErrorCode.AuthUsernameTaken
        "CURRENT_PASSWORD_INCORRECT" -> AppErrorCode.AuthChangePasswordFailed
        "AI_DAILY_QUOTA_EXCEEDED" -> AppErrorCode.AiQuotaExceeded
        "RATE_LIMITED" -> AppErrorCode.AiQuotaExceeded
        else -> null
    }
}
