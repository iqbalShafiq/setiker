package data.remote

import data.sync.ConnectivityState
import domain.error.DataError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.serialization.SerializationException

/**
 * Typed transport-failure classification for the Setiker API (spec P3b, D1).
 * Replaces the deleted string-matching classifier in `SyncManagerImpl` (`contains("timeout")`,
 * `contains("5")`).
 *
 * A failed connection does not mean the device is offline. The phone can be perfectly online and
 * still not reach the API, and telling the user the opposite is a message that costs trust, so
 * the connectivity state decides which of the two it is.
 *
 * Coroutine cancellation must NOT be classified: callers rethrow [CancellationException]
 * before calling this (see [runApiCall] and the sync executor catch order).
 */
fun Throwable.toDataError(): DataError.Network = when (this) {
    is ConnectTimeoutException ->
        if (ConnectivityState.isOnline) DataError.Network.ServerUnreachable else DataError.Network.ConnectTimeout
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
        isPlatformIoException() ->
            if (ConnectivityState.isOnline) DataError.Network.ServerUnreachable else DataError.Network.NoInternet
        else -> DataError.Network.Unknown
    }
}
