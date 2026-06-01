package data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiSuccessEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val meta: ApiMeta? = null
)

@Serializable
data class ApiErrorEnvelope(
    val success: Boolean,
    val error: ApiErrorPayload? = null,
    val meta: ApiMeta? = null
)

@Serializable
data class ApiErrorPayload(
    val code: String? = null,
    val subcode: String? = null,
    val message: String? = null,
    val details: List<String> = emptyList()
)

@Serializable
data class ApiMeta(
    val timestamp: String? = null,
    @SerialName("requestId") val requestId: String? = null
)

@Serializable
data class ApiTextOutsideForegroundStyle(
    val fontFamily: String? = null,
    val color: String? = null,
    val weight: String? = null
)

@Serializable
data class ApiTextOutsideForeground(
    val text: String? = null,
    val style: ApiTextOutsideForegroundStyle? = null
)

@Serializable
data class ApiImage(
    val id: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val textOutsideForeground: ApiTextOutsideForeground? = null,
    val textAssetDecoration: ApiTextAssetDecoration? = null
)
