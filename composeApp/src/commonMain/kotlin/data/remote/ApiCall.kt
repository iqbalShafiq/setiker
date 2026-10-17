package data.remote

import domain.error.DataError
import domain.error.Result
import kotlinx.coroutines.CancellationException
import util.AppLog

/**
 * The one pipeline for making a Setiker API call (spec P3b, D3): run the call, convert transport
 * failures into typed [DataError.Network], and log the failure (never a body) via [AppLog].
 *
 * Coroutine cancellation is rethrown untouched — cancelling a screen/job must not be turned
 * into an "API failure".
 */
internal suspend inline fun <T> runApiCall(
    tag: String,
    crossinline block: suspend () -> T,
): Result<T, DataError.Network> =
    try {
        Result.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        val typed = failure.toDataError()
        AppLog.d(tag, "api call failed: $typed")
        Result.Error(typed)
    }
