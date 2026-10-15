package data.remote

import domain.error.AppErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiErrorParserTest {
    @Test
    fun mapsAiConfigurationCodesToServiceUnavailable() {
        val notConfigured =
            """{"success":false,"error":{"code":"AI_NOT_CONFIGURED","message":"AGENT_API_KEY is required for AI features"}}"""
        assertEquals(AppErrorCode.AiServiceUnavailable, ApiErrorParser.parse(notConfigured)?.code)

        val modelMissing =
            """{"success":false,"error":{"code":"AI_MODEL_NOT_CONFIGURED","message":"AiModel row missing"}}"""
        assertEquals(AppErrorCode.AiServiceUnavailable, ApiErrorParser.parse(modelMissing)?.code)
    }

    @Test
    fun keepsQuotaSubcodeMapping() {
        val quota =
            """{"success":false,"error":{"code":"RATE_LIMITED","subcode":"AI_DAILY_QUOTA_EXCEEDED","message":"Daily AI limit reached"}}"""
        assertEquals(AppErrorCode.AiQuotaExceeded, ApiErrorParser.parse(quota)?.code)
    }

    @Test
    fun returnsNullForUnknownCodes() {
        val unknown = """{"success":false,"error":{"code":"SOMETHING_ELSE","message":"x"}}"""
        assertEquals(null, ApiErrorParser.parse(unknown)?.code)
    }
}
