package data.remote

import data.remote.model.ApiErrorEnvelope
import data.remote.model.ApiImage
import data.remote.model.ApiSuccessEnvelope
import data.remote.model.BackgroundRemoveData
import data.remote.model.GenerateData
import data.remote.model.GridSplitData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SetikerApiService(
    private val baseUrl: String = ApiConfig.baseUrl
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
        if (ApiConfig.isDebugLoggingEnabled) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("KtorClient: $message")
                    }
                }
                level = LogLevel.ALL
            }
        }
        install(DefaultRequest) {
            url(baseUrl)
            if (url.protocol.name.isBlank()) {
                url.protocol = URLProtocol.HTTP
            }
        }
    }

    suspend fun removeBackground(imagePath: String): ApiImage {
        val response = client.post("/api/v1/background/remove") {
            setMultipartBody(
                imagePath = imagePath
            )
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val apiError = runCatching { Json.decodeFromString<ApiErrorEnvelope>(bodyText) }.getOrNull()
            throw ApiException(apiError?.error?.message ?: "Background remover request failed")
        }
        val parsed = Json.decodeFromString<ApiSuccessEnvelope<BackgroundRemoveData>>(bodyText)
        return parsed.data?.image ?: throw ApiException("Invalid remove background response")
    }

    suspend fun generate(
        prompt: String,
        grid: Boolean = true,
        gridLayout: String? = "4x4",
        normalize: Boolean? = true
    ): List<ApiImage> {
        val response = client.post("/api/v1/generate") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("text", prompt)
                        append("grid", grid.toString())
                        if (grid) {
                            gridLayout?.let { append("layout", it) }
                            normalize?.let { append("normalize", it.toString()) }
                        }
                    }
                )
            )
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val apiError = runCatching { Json.decodeFromString<ApiErrorEnvelope>(bodyText) }.getOrNull()
            throw ApiException(apiError?.error?.message ?: "Generate request failed")
        }
        val parsed = Json.decodeFromString<ApiSuccessEnvelope<GenerateData>>(bodyText)
        return parsed.data?.images ?: throw ApiException("Invalid generate response")
    }

    suspend fun splitGrid(imagePath: String): List<ApiImage> {
        val response = client.post("/api/v1/grid/split") {
            setMultipartBody(imagePath = imagePath)
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val apiError = runCatching { Json.decodeFromString<ApiErrorEnvelope>(bodyText) }.getOrNull()
            throw ApiException(apiError?.error?.message ?: "Grid split request failed")
        }
        val parsed = Json.decodeFromString<ApiSuccessEnvelope<GridSplitData>>(bodyText)
        return parsed.data?.images ?: throw ApiException("Invalid grid split response")
    }

    suspend fun downloadImageBytes(urlPath: String): ByteArray {
        val response = client.get(urlPath)
        if (!response.status.isSuccess()) {
            throw ApiException("Failed to download generated image")
        }
        return response.body()
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
