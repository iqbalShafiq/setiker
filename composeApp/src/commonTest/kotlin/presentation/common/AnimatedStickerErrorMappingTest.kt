package presentation.common

import domain.util.AnimatedStickerValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.animated_error_draft_not_found
import setiker.composeapp.generated.resources.animated_error_file_too_large
import setiker.composeapp.generated.resources.animated_error_fps_out_of_range

class AnimatedStickerErrorMappingTest {

    @Test
    fun validatorReasonMapsToResource() {
        val uiText = AnimatedStickerValidator.Reason.FpsOutOfRange.toUiText()
        assertEquals(Res.string.animated_error_fps_out_of_range, (uiText as UiText.StringRes).resource)
    }

    @Test
    fun failureMessageMapsKnownCopy() {
        val uiText = animatedFailureMessageToUiText("Draft not found. Please pick the video again.")
        assertNotNull(uiText)
        assertEquals(Res.string.animated_error_draft_not_found, (uiText as UiText.StringRes).resource)
    }

    @Test
    fun fileSizeReasonMapsToFileTooLargeString() {
        val uiText = AnimatedStickerValidator.Reason.FileSizeTooLarge.toUiText()
        assertEquals(Res.string.animated_error_file_too_large, (uiText as UiText.StringRes).resource)
    }
}
