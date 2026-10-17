package data.remote

import domain.error.AppError
import domain.error.DataError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Typed transport-failure classification for the Setiker API (spec P3b, D1).
 * Replaces the deleted string-matching classifier in `SyncManagerImpl` (`contains("timeout")`,
 * `contains("5")`).
 *
 * Coroutine cancellation must NOT be classified: callers rethrow [CancellationException]
 * before calling this (see [runApiCall] and the sync executor catch order).
 */
fun Throwable.toDataError(): DataError.Network = when (this) {
    is ConnectTimeoutException -> DataError.Network.ConnectTimeout
    is SocketTimeoutException -> DataError.Network.RequestTimeout
    is HttpRequestTimeoutException -> DataError.Network.RequestTimeout
    is ServerResponseException -> DataError.Network.HttpFailure(status = response.status.value)
    is ClientRequestException -> DataError.Network.HttpFailure(status = response.status.value)
    is ApiException -> DataError.Network.ApiRejected(
        appCode = code,
        message = message,
        subcode = subcode,
    )
    is SerializationException -> DataError.Network.Serialization
    else -> when {
        isPlatformIoException() -> DataError.Network.NoInternet
        else -> DataError.Network.Unknown
    }
}
