package data.remote.explore

import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse

internal enum class ExploreAuthMode { Public, Optional, Required }

/**
 * Internal plumbing shared by the Explore data sources (spec P3b, D5, façade retained):
 * the Ktor client and base URL owned by `ExploreApiRepository`, plus the
 * once-per-request 401-refresh wrapper. Decoding stays with each source via `ApiJson.codec`.
 */
internal interface ExploreGateway {
    val http: HttpClient
    val baseUrl: String

    /**
     * Builds and issues one request; applies the token-refresh-once-on-401 logic of
     * `withOptionalAuthRetry` / `withRequiredAuthRetry` (which this replaces):
     * [ExploreAuthMode.Required] throws `ApiException(AuthNotAuthenticated)` when no token is
     * available, [ExploreAuthMode.Optional] proceeds unauthenticated, [ExploreAuthMode.Public]
     * never sends an Authorization header.
     */
    suspend fun authRetry(
        mode: ExploreAuthMode,
        request: suspend (authHeader: String?) -> HttpResponse,
    ): HttpResponse

    fun extractMessage(bodyText: String): String?
}
