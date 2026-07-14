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
        "INVALID_GOOGLE_TOKEN" -> AppErrorCode.AuthGoogleFailed
        "EMAIL_NOT_VERIFIED" -> AppErrorCode.AuthGoogleFailed
        "ACCOUNT_EXISTS_PASSWORD" -> AppErrorCode.AuthAccountExistsPassword
        "USE_GOOGLE_SIGN_IN" -> AppErrorCode.AuthUseGoogleSignIn
        "GOOGLE_ALREADY_LINKED" -> AppErrorCode.AuthGoogleFailed
        "NO_PASSWORD_SET" -> AppErrorCode.AuthNoPasswordSet
        "CANNOT_UNLINK_SOLE_AUTH" -> AppErrorCode.AuthCannotUnlinkSoleAuth
        "AI_DAILY_QUOTA_EXCEEDED" -> AppErrorCode.AiQuotaExceeded
        "RATE_LIMITED" -> AppErrorCode.AiQuotaExceeded
        else -> null
    }
}
