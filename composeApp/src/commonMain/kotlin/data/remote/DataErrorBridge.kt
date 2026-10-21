package data.remote

import domain.error.AppErrorCode
import domain.error.DataError
import domain.error.detailText

/**
 * Bridges a typed [DataError.Network] back to the exception shape required by the
 * domain-contract repositories ([data.billing.BillingRepository] and
 * [domain.repository.AiQuotaRepository]) whose consumers still branch on
 * `AppException.code` — `AiJobFailureClassifier` distinguishes quota exhaustion (R4)
 * from generic AI failures by reading it, so the parsed app code must survive the
 * typed boundary.
 *
 * `429` without a parsed body code maps to [AppErrorCode.AiQuotaExceeded]: the only
 * rate-limited surface in the app is AI quota (`ApiErrorParser` maps `RATE_LIMITED`
 * the same way). The [fallback] code is used when the failure carries no business code.
 */
fun DataError.Network.toApiException(
    fallback: AppErrorCode = AppErrorCode.CloudFetchFailed,
): ApiException = when (this) {
    is DataError.Network.ApiRejected -> ApiException(
        code = appCode ?: fallback,
        message = message ?: detailText(),
        subcode = subcode,
    )
    is DataError.Network.HttpFailure -> ApiException(
        code = appCode ?: if (status == 429) AppErrorCode.AiQuotaExceeded else fallback,
        message = message ?: detailText(),
        subcode = subcode,
    )
    else -> ApiException(fallback, detailText())
}
