package presentation.common

import domain.error.AppErrorCode
import domain.error.AppException
import org.jetbrains.compose.resources.StringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_background_remove_request_failed
import setiker.composeapp.generated.resources.error_generate_request_failed
import setiker.composeapp.generated.resources.error_grid_split_request_failed
import setiker.composeapp.generated.resources.error_image_download_failed
import setiker.composeapp.generated.resources.error_invalid_background_remove_response
import setiker.composeapp.generated.resources.error_invalid_generate_response
import setiker.composeapp.generated.resources.error_invalid_grid_split_response
import setiker.composeapp.generated.resources.error_pack_not_found
import setiker.composeapp.generated.resources.error_sticker_not_found

fun Throwable.toUiText(defaultRes: StringResource): UiText {
    val appException = this as? AppException
    val resource = appException?.code?.toStringResource() ?: defaultRes
    return UiText.StringRes(resource)
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
    }
}
