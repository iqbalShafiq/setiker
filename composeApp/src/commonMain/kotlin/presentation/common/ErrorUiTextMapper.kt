package presentation.common

import data.remote.ApiException
import domain.error.AppErrorCode
import domain.error.AppException
import domain.error.serverSubcodeToAppErrorCode
import org.jetbrains.compose.resources.StringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_ai_quota_exceeded
import setiker.composeapp.generated.resources.error_auth_account_deactivated
import setiker.composeapp.generated.resources.error_auth_account_exists_password
import setiker.composeapp.generated.resources.error_auth_account_exists_other_provider
import setiker.composeapp.generated.resources.error_auth_apple_failed
import setiker.composeapp.generated.resources.error_auth_cannot_unlink_sole
import setiker.composeapp.generated.resources.error_auth_change_password_failed
import setiker.composeapp.generated.resources.error_auth_google_failed
import setiker.composeapp.generated.resources.error_auth_invalid_reset_token
import setiker.composeapp.generated.resources.error_auth_login_failed
import setiker.composeapp.generated.resources.error_auth_no_password_set
import setiker.composeapp.generated.resources.error_auth_not_authenticated
import setiker.composeapp.generated.resources.error_auth_password_reset_failed
import setiker.composeapp.generated.resources.error_auth_profile_failed
import setiker.composeapp.generated.resources.error_auth_refresh_failed
import setiker.composeapp.generated.resources.error_auth_register_failed
import setiker.composeapp.generated.resources.error_auth_use_oauth_or_set_password
import setiker.composeapp.generated.resources.error_edit_profile_failed
import setiker.composeapp.generated.resources.error_username_taken
import setiker.composeapp.generated.resources.error_background_remove_request_failed
import setiker.composeapp.generated.resources.error_cloud_create_failed
import setiker.composeapp.generated.resources.error_cloud_delete_failed
import setiker.composeapp.generated.resources.error_cloud_fetch_failed
import setiker.composeapp.generated.resources.error_cloud_sync_failed
import setiker.composeapp.generated.resources.error_cloud_update_failed
import setiker.composeapp.generated.resources.error_generate_request_failed
import setiker.composeapp.generated.resources.error_grid_split_request_failed
import setiker.composeapp.generated.resources.error_image_download_failed
import setiker.composeapp.generated.resources.error_invalid_background_remove_response
import setiker.composeapp.generated.resources.error_invalid_generate_response
import setiker.composeapp.generated.resources.error_invalid_grid_split_response
import setiker.composeapp.generated.resources.error_network_unavailable
import setiker.composeapp.generated.resources.error_pack_not_found
import setiker.composeapp.generated.resources.error_sticker_not_found

fun Throwable.toUiText(defaultRes: StringResource): UiText {
    val code = (this as? ApiException)?.code ?: (this as? AppException)?.code
    return code?.toUiText() ?: UiText.StringRes(defaultRes)
}

fun AppErrorCode.toUiText(): UiText = UiText.StringRes(toStringResource())

fun serverSubcodeToUiText(subcode: String?): UiText? {
    return serverSubcodeToAppErrorCode(subcode)?.toUiText()
}

private fun AppErrorCode.toStringResource(): StringResource {
    return when (this) {
        AppErrorCode.BackgroundRemoveRequestFailed -> Res.string.error_background_remove_request_failed
        AppErrorCode.InvalidBackgroundRemoveResponse -> Res.string.error_invalid_background_remove_response
        AppErrorCode.GenerateRequestFailed -> Res.string.error_generate_request_failed
        AppErrorCode.InvalidGenerateResponse -> Res.string.error_invalid_generate_response
        AppErrorCode.GridSplitRequestFailed -> Res.string.error_grid_split_request_failed
        AppErrorCode.InvalidGridSplitResponse -> Res.string.error_invalid_grid_split_response
        AppErrorCode.ImageDownloadFailed -> Res.string.error_image_download_failed
        AppErrorCode.PackNotFound -> Res.string.error_pack_not_found
        AppErrorCode.StickerNotFound -> Res.string.error_sticker_not_found
        AppErrorCode.AuthLoginFailed -> Res.string.error_auth_login_failed
        AppErrorCode.AuthRegisterFailed -> Res.string.error_auth_register_failed
        AppErrorCode.AuthRefreshFailed -> Res.string.error_auth_refresh_failed
        AppErrorCode.AuthProfileFailed -> Res.string.error_auth_profile_failed
        AppErrorCode.AuthUpdateProfileFailed -> Res.string.error_edit_profile_failed
        AppErrorCode.AuthUsernameTaken -> Res.string.error_username_taken
        AppErrorCode.AuthChangePasswordFailed -> Res.string.error_auth_change_password_failed
        AppErrorCode.AuthAccountDeactivated -> Res.string.error_auth_account_deactivated
        AppErrorCode.AuthNotAuthenticated -> Res.string.error_auth_not_authenticated
        AppErrorCode.AuthGoogleFailed -> Res.string.error_auth_google_failed
        AppErrorCode.AuthAppleFailed -> Res.string.error_auth_apple_failed
        AppErrorCode.AuthUseGoogleSignIn -> Res.string.error_auth_use_oauth_or_set_password
        AppErrorCode.AuthUseOauthOrSetPassword -> Res.string.error_auth_use_oauth_or_set_password
        AppErrorCode.AuthAccountExistsPassword -> Res.string.error_auth_account_exists_password
        AppErrorCode.AuthAccountExistsOtherProvider -> Res.string.error_auth_account_exists_other_provider
        AppErrorCode.AuthNoPasswordSet -> Res.string.error_auth_no_password_set
        AppErrorCode.AuthCannotUnlinkSoleAuth -> Res.string.error_auth_cannot_unlink_sole
        AppErrorCode.AuthPasswordResetFailed -> Res.string.error_auth_password_reset_failed
        AppErrorCode.AuthInvalidPasswordResetToken -> Res.string.error_auth_invalid_reset_token
        AppErrorCode.AiQuotaExceeded -> Res.string.error_ai_quota_exceeded
        AppErrorCode.CloudFetchFailed -> Res.string.error_cloud_fetch_failed
        AppErrorCode.CloudCreateFailed -> Res.string.error_cloud_create_failed
        AppErrorCode.CloudUpdateFailed -> Res.string.error_cloud_update_failed
        AppErrorCode.CloudDeleteFailed -> Res.string.error_cloud_delete_failed
        AppErrorCode.CloudSyncFailed -> Res.string.error_cloud_sync_failed
    }
}
