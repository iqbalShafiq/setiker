package presentation.common

import data.remote.ApiException
import domain.error.AppErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_auth_login_failed
import setiker.composeapp.generated.resources.error_ai_quota_exceeded

class ErrorUiTextMapperTest {

    @Test
    fun appErrorCodeMapsToExpectedResource() {
        val uiText = AppErrorCode.AuthLoginFailed.toUiText()
        assertEquals(Res.string.error_auth_login_failed, (uiText as UiText.StringRes).resource)
    }

    @Test
    fun apiExceptionUsesMappedCode() {
        val uiText = ApiException(AppErrorCode.AiQuotaExceeded).toUiText(Res.string.error_auth_login_failed)
        assertEquals(Res.string.error_ai_quota_exceeded, (uiText as UiText.StringRes).resource)
    }

    @Test
    fun serverSubcodeMapsToQuota() {
        val uiText = serverSubcodeToUiText("AI_DAILY_QUOTA_EXCEEDED")
        assertNotNull(uiText)
        assertEquals(Res.string.error_ai_quota_exceeded, (uiText as UiText.StringRes).resource)
    }
}
