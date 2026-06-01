package data.remote

import data.remote.model.ApiErrorEnvelope
import domain.error.AppErrorCode
import domain.error.serverSubcodeToAppErrorCode
import kotlinx.serialization.json.Json

object ApiErrorParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun extractMessage(bodyText: String): String? {
        return parse(bodyText)?.message
    }

    fun parse(bodyText: String): ParsedApiError? {
        val envelope = runCatching { json.decodeFromString<ApiErrorEnvelope>(bodyText) }.getOrNull()
            ?: return null
        val error = envelope.error ?: return null
        val code = serverSubcodeToAppErrorCode(error.subcode)
            ?: when (error.code) {
                "RATE_LIMITED" -> AppErrorCode.AiQuotaExceeded
                "UNAUTHORIZED" -> AppErrorCode.AuthNotAuthenticated
                "CONFLICT" -> AppErrorCode.AuthRegisterFailed
                else -> null
            }
        return ParsedApiError(
            code = code,
            message = error.message,
            subcode = error.subcode
        )
    }
}

data class ParsedApiError(
    val code: AppErrorCode?,
    val message: String?,
    val subcode: String? = null
)
