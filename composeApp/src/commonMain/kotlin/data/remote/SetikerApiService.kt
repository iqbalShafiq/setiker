package data.remote

import data.remote.model.ApiErrorEnvelope
import data.remote.model.ApiImage
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.GenerateData
import data.remote.model.GridSplitTextAssetsData
import data.remote.model.ApiTextAsset
import data.remote.model.VideoStickerPackPlanData
import domain.model.VideoStickerCandidateManifestItem
import domain.error.AppErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import data.auth.AuthManager
import data.auth.AuthTokenRefresher
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

class SetikerApiService(
    private val authManager: AuthManager? = null,
    private val authTokenRefresher: AuthTokenRefresher? = null,
    private val baseUrl: String = ApiConfig.baseUrl
) {
    companion object {
        const val AI_RESERVATION_HEADER = "X-AI-Reservation-Id"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun HttpRequestBuilder.applyAiReservation(reservationId: String?) {
        reservationId?.takeIf { it.isNotBlank() }?.let { header(AI_RESERVATION_HEADER, it) }
    }

    private fun mapApiFailure(bodyText: String, defaultCode: AppErrorCode): ApiException {
        val parsed = ApiErrorParser.parse(bodyText)
        return ApiException(
            code = parsed?.code ?: defaultCode,
            message = parsed?.message
        )
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                json
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
        if (ApiConfig.isDebugLoggingEnabled) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("KtorClient: $message")
                    }
                }
                sanitizeHeader { it == HttpHeaders.Authorization }
                level = LogLevel.ALL
            }
        }
        install(DefaultRequest) {
            url(baseUrl)
            if (url.protocol.name.isBlank()) {
                url.protocol = URLProtocol.HTTPS
            }
        }
    }

    private suspend fun resolveAccessToken(): String? {
        val manager = authManager ?: return null

        manager.getValidAccessToken()?.let { return it }

        return authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = false)
            ?: manager.getAccessToken()
    }

    private suspend fun withAuthRetry(request: suspend (String?) -> HttpResponse): HttpResponse {
        val firstToken = resolveAccessToken()
        val firstResponse = request(firstToken?.let { "Bearer $it" })
        if (firstResponse.status != HttpStatusCode.Unauthorized) {
            return firstResponse
        }

        val refreshedToken = authTokenRefresher?.refreshAccessToken(clearTokensOnFailure = true)
            ?: return firstResponse
        return request("Bearer $refreshedToken")
    }

    suspend fun generate(
        prompt: String,
        inputImagePath: String? = null,
        reservationId: String? = null
    ): List<ApiImage> {
        val response = withAuthRetry { authHeader ->
            client.post("/api/v1/generate") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
                applyAiReservation(reservationId)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("text", prompt)
                            if (!inputImagePath.isNullOrBlank()) {
                                appendImageFile(key = "image", path = inputImagePath, filename = "input.png")
                            }
                        }
                    )
                )
            }
        }
        return parseGenerateImages(response)
    }

    suspend fun generateStickerPack(
        prompt: String,
        layout: String,
        inputImagePath: String? = null,
        reservationId: String? = null
    ): List<ApiImage> {
        val response = withAuthRetry { authHeader ->
            client.post("/api/v1/generate/sticker-pack") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
                applyAiReservation(reservationId)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("text", prompt)
                            append("layout", layout)
                            if (!inputImagePath.isNullOrBlank()) {
                                appendImageFile(key = "image", path = inputImagePath, filename = "input.png")
                            }
                        }
                    )
                )
            }
        }
        return parseGenerateImages(response)
    }

    suspend fun generateVideoStickerPack(
        candidateGridPaths: List<String>,
        candidateManifest: List<VideoStickerCandidateManifestItem>,
        selectedStartMs: Long,
        selectedEndMs: Long,
        sourceDurationMs: Long,
        prompt: String? = null,
        reservationId: String? = null
    ) = generateVideoStickerPack(
        candidateGridPaths = candidateGridPaths,
        candidateManifest = candidateManifest,
        selectedStartMs = selectedStartMs,
        selectedEndMs = selectedEndMs,
        sourceDurationMs = sourceDurationMs,
        prompt = prompt,
        maxStaticStickers = null,
        maxAnimatedStickers = null,
        reservationId = reservationId
    )

    suspend fun generateVideoStickerPack(
        candidateGridPaths: List<String>,
        candidateManifest: List<VideoStickerCandidateManifestItem>,
        selectedStartMs: Long,
        selectedEndMs: Long,
        sourceDurationMs: Long,
        prompt: String? = null,
        maxStaticStickers: Int? = null,
        maxAnimatedStickers: Int? = null,
        reservationId: String? = null
    ): data.remote.model.ApiVideoStickerPackPlan {
        val response = withAuthRetry { authHeader ->
            client.post("/api/v1/generate/video-sticker-pack") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
                applyAiReservation(reservationId)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("layout", "4x4")
                            append("candidateLayout", "4x4")
                            append("candidateManifest", json.encodeToString(candidateManifest))
                            append("selectedStartMs", selectedStartMs.toString())
                            append("selectedEndMs", selectedEndMs.toString())
                            append("sourceDurationMs", sourceDurationMs.toString())
                            if (!prompt.isNullOrBlank()) {
                                append("prompt", prompt)
                            }
                            maxStaticStickers?.let { append("maxStaticStickers", it.toString()) }
                            maxAnimatedStickers?.let { append("maxAnimatedStickers", it.toString()) }
                            candidateGridPaths.forEachIndexed { i, path ->
                                appendImageFile(
                                    key = "candidate_grids",
                                    path = path,
                                    filename = "candidate_grid_${i}.png"
                                )
                            }
                        }
                    )
                )
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw mapApiFailure(bodyText, AppErrorCode.GenerateRequestFailed)
        }
        return json.decodeFromString<ApiSuccessEnvelope<VideoStickerPackPlanData>>(bodyText).data?.plan
            ?: throw ApiException(code = AppErrorCode.InvalidGenerateResponse)
    }

    suspend fun improve(
        imagePaths: List<String>,
        reservationId: String? = null
    ): List<ApiImage> {
        val response = withAuthRetry { authHeader ->
            client.post("/api/v1/generate/improvement") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
                applyAiReservation(reservationId)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            imagePaths.forEachIndexed { index, path ->
                                appendImageFile(
                                    key = "images",
                                    path = path,
                                    filename = "image_${index + 1}.png"
                                )
                            }
                        }
                    )
                )
            }
        }
        return parseGenerateImages(response)
    }

    private suspend fun parseGenerateImages(response: HttpResponse): List<ApiImage> {
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw mapApiFailure(bodyText, AppErrorCode.GenerateRequestFailed)
        }
        val parsed = json.decodeFromString<ApiSuccessEnvelope<GenerateData>>(bodyText)
        return parsed.data?.images
            ?: throw ApiException(code = AppErrorCode.InvalidGenerateResponse)
    }

    suspend fun extractGridTextAssets(imagePaths: List<String>): List<ApiTextAsset> {
        if (imagePaths.isEmpty()) return emptyList()

        val response = withAuthRetry { authHeader ->
            client.post("/api/v1/grid/split/text-assets") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            imagePaths.forEachIndexed { index, path ->
                                val bytes = readFileBytes(path)
                                append(
                                    key = "images",
                                    value = bytes,
                                    headers = io.ktor.http.Headers.build {
                                        append(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
                                        append(
                                            HttpHeaders.ContentDisposition,
                                            "filename=\"cell_${index + 1}.png\""
                                        )
                                    }
                                )
                            }
                        }
                    )
                )
                contentType(ContentType.MultiPart.FormData)
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiException(
                code = AppErrorCode.GridSplitRequestFailed,
                message = runCatching { json.decodeFromString<ApiErrorEnvelope>(bodyText) }
                    .getOrNull()
                    ?.error
                    ?.message
            )
        }
        val parsed = json.decodeFromString<ApiSuccessEnvelope<GridSplitTextAssetsData>>(bodyText)
        return parsed.data?.assets
            ?: throw ApiException(code = AppErrorCode.InvalidGridSplitResponse)
    }

    suspend fun downloadImageBytes(urlPath: String): ByteArray {
        var lastError: Throwable? = null

        repeat(3) { attempt ->
            try {
                val requestUrl = normalizeDownloadPath(urlPath)
                val response = client.get(requestUrl)
                if (response.status.isSuccess()) {
                    return response.body()
                }
                lastError = ApiException(code = AppErrorCode.ImageDownloadFailed)
            } catch (e: Throwable) {
                println("SetikerApiService: download attempt ${attempt + 1} failed for path=$urlPath reason=${e.message}")
                lastError = e
            }

            if (attempt < 2) {
                // Backend may return image URLs before the file is immediately readable.
                delay(250L * (attempt + 1))
            }
        }

        val appError = lastError as? ApiException
        throw ApiException(code = appError?.code ?: AppErrorCode.ImageDownloadFailed)
    }

    private fun normalizeDownloadPath(urlPath: String): String {
        if (urlPath.startsWith("http://") || urlPath.startsWith("https://")) {
            return urlPath
        }
        return if (urlPath.startsWith("/")) urlPath else "/$urlPath"
    }

    private fun FormBuilder.appendImageFile(
        key: String,
        path: String,
        filename: String
    ) {
        val bytes = readFileBytes(path)
        append(
            key = key,
            value = bytes,
            headers = io.ktor.http.Headers.build {
                append(HttpHeaders.ContentType, ContentType.Image.Any.toString())
                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
            }
        )
    }

    private fun io.ktor.client.request.HttpRequestBuilder.setMultipartBody(
        imagePath: String
    ) {
        val imageBytes = readFileBytes(imagePath)
        setBody(
            MultiPartFormDataContent(
                formData {
                    append(
                        key = "image",
                        value = imageBytes,
                        headers = io.ktor.http.Headers.build {
                            append(HttpHeaders.ContentType, ContentType.Image.Any.toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"upload.png\"")
                        }
                    )
                }
            )
        )
        contentType(ContentType.MultiPart.FormData)
    }
}
