package domain.error

/** Marker for typed application failures. `kotlin.Result` (stdlib) is never used for these signatures. */
interface AppError

/**
 * Typed failure model for the data layer, per `docs/specs/p3b-data-layer-consolidation.md` (D1).
 *
 * Deviation from the spec text (documented as DD1): the HTTP-status enumeration
 * (`BadRequest`/`NotFound`/`Conflict`/`RateLimited`/`Server`, ...) is collapsed into
 * [DataError.Network.HttpFailure] carrying the status plus the parsed [AppErrorCode] so the
 * status logic (retryability, auth flow) is decided in one place. Predicates
 * (`isUnauthorized`, `isRateLimited`, `isServerError`) keep call sites readable.
 */
sealed interface DataError : AppError {

    /** Local (disk/serialization) failures. The sync loop never retries these. */
    enum class Local : DataError {
        DiskFull,
        DiskRead,
        DiskWrite,
        Serialization,
        Unknown,
    }

    sealed interface Network : DataError {
        /** Decided per member here so the sync executor has exactly one retry rule. */
        val retryable: Boolean

        data object NoInternet : Network { override val retryable: Boolean get() = true }
        data object ConnectTimeout : Network { override val retryable: Boolean get() = true }
        data object RequestTimeout : Network { override val retryable: Boolean get() = true }
        data object Serialization : Network { override val retryable: Boolean get() = false }
        data object Unknown : Network { override val retryable: Boolean get() = false }

        /**
         * The server answered with an HTTP status. [appCode] is the parsed business code
         * (from `ApiErrorParser`) when the body carried one; [subcode] is the raw value.
         */
        data class HttpFailure(
            val status: Int?,
            val appCode: AppErrorCode? = null,
            val message: String? = null,
            val subcode: String? = null,
        ) : Network {
            override val retryable: Boolean
                get() = status == 429 || (status != null && status >= 500)
        }

        /**
         * A business rejection represented by [data.remote.ApiException]: no transport status is
         * available at classification time, so it is treated as permanent.
         */
        data class ApiRejected(
            val appCode: AppErrorCode? = null,
            val message: String? = null,
            val subcode: String? = null,
        ) : Network {
            override val retryable: Boolean get() = false
        }
    }
}

/** One-line message used for DB error columns and logs; no UI text lives here. */
fun DataError.detailText(): String = when (this) {
    is DataError.Network.HttpFailure ->
        message ?: (status?.let { "HTTP $it" } ?: "HTTP failure code=${appCode?.name ?: "unknown"}")
    is DataError.Network.ApiRejected -> message ?: appCode?.name ?: "API rejected"
    is DataError.Network.NoInternet -> "Network unavailable"
    is DataError.Network.ConnectTimeout -> "Connection timed out"
    is DataError.Network.RequestTimeout -> "Request timed out"
    is DataError.Network.Serialization -> "Malformed response body"
    is DataError.Network.Unknown -> "Unknown network error"
    is DataError.Local -> name
}

fun DataError.Network.isUnauthorized(): Boolean =
    this is DataError.Network.HttpFailure && status == 401

fun DataError.Network.isRateLimited(): Boolean =
    this is DataError.Network.HttpFailure && status == 429

fun DataError.Network.isServerError(): Boolean =
    this is DataError.Network.HttpFailure && (status == null || status >= 500)
