package domain.error

enum class AppErrorCode {
    BackgroundRemoveRequestFailed,
    InvalidBackgroundRemoveResponse,
    GenerateRequestFailed,
    InvalidGenerateResponse,
    GridSplitRequestFailed,
    InvalidGridSplitResponse,
    ImageDownloadFailed,
    PackNotFound,
    StickerNotFound,
    AuthLoginFailed("AUTH_LOGIN_FAILED"),
    AuthRegisterFailed("AUTH_REGISTER_FAILED"),
    AuthRefreshFailed("AUTH_REFRESH_FAILED"),
    AuthProfileFailed("AUTH_PROFILE_FAILED"),
    AuthUpdateProfileFailed("AUTH_UPDATE_PROFILE_FAILED"),
    AuthUsernameTaken("AUTH_USERNAME_TAKEN"),
    AuthChangePasswordFailed("AUTH_CHANGE_PASSWORD_FAILED"),
    AuthAccountDeactivated("AUTH_ACCOUNT_DEACTIVATED"),
    AuthNotAuthenticated("AUTH_NOT_AUTHENTICATED"),
    AuthGoogleFailed("AUTH_GOOGLE_FAILED"),
    AuthUseGoogleSignIn("AUTH_USE_GOOGLE_SIGN_IN"),
    AuthAccountExistsPassword("AUTH_ACCOUNT_EXISTS_PASSWORD"),
    AuthNoPasswordSet("AUTH_NO_PASSWORD_SET"),
    AuthCannotUnlinkSoleAuth("AUTH_CANNOT_UNLINK_SOLE_AUTH"),
    AiQuotaExceeded("AI_QUOTA_EXCEEDED"),
    CloudFetchFailed("CLOUD_FETCH_FAILED"),
    CloudCreateFailed("CLOUD_CREATE_FAILED"),
    CloudUpdateFailed("CLOUD_UPDATE_FAILED"),
    CloudDeleteFailed("CLOUD_DELETE_FAILED"),
    CloudSyncFailed("CLOUD_SYNC_FAILED");

    val errorCode: String

    constructor() {
        this.errorCode = name
    }

    constructor(errorCode: String) {
        this.errorCode = errorCode
    }
}
