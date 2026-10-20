package presentation.aijobs

import domain.model.aijob.AiJob
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AiJobProgress
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * F1: an active AI job must always show *some* progress affordance.
 *
 * Previously the progress bar was only rendered when the API happened to send a `progress`
 * payload, so a running job could render as nothing but a static status line.
 */
class AiJobCardPresentationTest {

    @Test
    fun `a running job without a progress payload still shows an indicator`() {
        assertEquals(
            AiJobProgressMode.INDETERMINATE,
            aiJobProgressMode(status = AiJobStatus.RUNNING, hasProgress = false)
        )
    }

    @Test
    fun `a queued job without a progress payload still shows an indicator`() {
        assertEquals(
            AiJobProgressMode.INDETERMINATE,
            aiJobProgressMode(status = AiJobStatus.QUEUED, hasProgress = false)
        )
    }

    @Test
    fun `an active job with a progress payload shows a determinate bar`() {
        assertEquals(
            AiJobProgressMode.DETERMINATE,
            aiJobProgressMode(status = AiJobStatus.RUNNING, hasProgress = true)
        )
    }

    @Test
    fun `every active status shows an indicator`() {
        val active = listOf(
            AiJobStatus.QUEUED,
            AiJobStatus.RUNNING,
            AiJobStatus.WAITING_FOR_NETWORK,
            AiJobStatus.CHECKPOINTED,
            AiJobStatus.CANCEL_REQUESTED
        )

        active.forEach { status ->
            assertTrue(
                aiJobProgressMode(status = status, hasProgress = false) != AiJobProgressMode.NONE,
                "$status must show a progress indicator"
            )
        }
    }

    @Test
    fun `a finished job shows no indicator`() {
        listOf(
            AiJobStatus.COMPLETED,
            AiJobStatus.APPLIED,
            AiJobStatus.CANCELLED,
            AiJobStatus.FAILED_RETRYABLE,
            AiJobStatus.FAILED_FINAL
        ).forEach { status ->
            assertEquals(
                AiJobProgressMode.NONE,
                aiJobProgressMode(status = status, hasProgress = false),
                "$status must not show a progress indicator"
            )
        }
    }

    @Test
    fun `a missing job shows no indicator`() {
        assertEquals(AiJobProgressMode.NONE, aiJobProgressMode(status = null, hasProgress = false))
    }

    // F1 (follow-up): a finished job must not keep advertising its last step.
    @Test
    fun `a finished job does not report its last step as the current step`() {
        val job = job(AiJobStatus.COMPLETED, stepLabel = "Removing background")

        assertNull(activeJobStepLabel(job))
    }

    @Test
    fun `a running job reports its current step`() {
        val job = job(AiJobStatus.RUNNING, stepLabel = "Removing background")

        assertEquals("Removing background", activeJobStepLabel(job))
    }

    @Test
    fun `a queued job without a step label reports nothing`() {
        val job = job(AiJobStatus.QUEUED, stepLabel = "")

        assertNull(activeJobStepLabel(job))
    }

    private fun job(status: AiJobStatus, stepLabel: String) = AiJob(
        id = "job-1",
        workspaceDraftId = "draft-1",
        type = AiJobType.REMOVE_BACKGROUND,
        status = status,
        origin = AiJobOrigin.EDITOR,
        payloadJson = "{}",
        progress = AiJobProgress(
            stepKey = "removing_background",
            stepLabel = stepLabel,
            fraction = 0.5f
        ),
        attemptGroupId = "group-1",
        createdAt = 0L,
        updatedAt = 0L
    )

    // F19: the failure message was rendered twice — once as the status line and once as the detail.
    @Test
    fun `the failure detail is hidden when it repeats the status line`() {
        assertFalse(
            shouldShowFailureDetail(
                statusLabel = "Processing was interrupted",
                failureMessage = "Processing was interrupted",
                progressMode = AiJobProgressMode.NONE
            )
        )
    }

    @Test
    fun `the failure detail is shown when it adds information`() {
        assertTrue(
            shouldShowFailureDetail(
                statusLabel = "Needs attention",
                failureMessage = "Connect timeout has expired",
                progressMode = AiJobProgressMode.NONE
            )
        )
    }

    @Test
    fun `the failure detail is hidden while a progress indicator is showing`() {
        assertFalse(
            shouldShowFailureDetail(
                statusLabel = "Removing background",
                failureMessage = "Connect timeout has expired",
                progressMode = AiJobProgressMode.INDETERMINATE
            )
        )
    }
}
