package presentation.aijobs

import domain.model.aijob.AiJob
import domain.model.aijob.AiJobStatus

/**
 * How the AI-jobs list card should render the "in flight" state of a job.
 *
 * Before this existed the card only drew a progress bar when the API happened to include a
 * `progress` payload, so a job that was genuinely running could render as a static line of text
 * with no motion at all.
 */
enum class AiJobProgressMode {
    /** The job is not running: nothing to show. */
    NONE,

    /** The job is running and the server knows how far along it is. */
    DETERMINATE,

    /** The job is running but the server has no progress information yet. */
    INDETERMINATE
}

/** Job statuses that mean "this job is still in flight". */
internal val ACTIVE_JOB_STATUSES: Set<AiJobStatus> = setOf(
    AiJobStatus.QUEUED,
    AiJobStatus.RUNNING,
    AiJobStatus.WAITING_FOR_NETWORK,
    AiJobStatus.CHECKPOINTED,
    AiJobStatus.CANCEL_REQUESTED
)

/**
 * Resolves the progress affordance for a job.
 *
 * @param status the job status, or `null` when the draft has no job yet.
 * @param hasProgress whether the server sent a progress payload with a usable fraction.
 */
internal fun aiJobProgressMode(status: AiJobStatus?, hasProgress: Boolean): AiJobProgressMode {
    if (status == null || status !in ACTIVE_JOB_STATUSES) return AiJobProgressMode.NONE
    return if (hasProgress) AiJobProgressMode.DETERMINATE else AiJobProgressMode.INDETERMINATE
}

/**
 * The step label of a job that is *still running*.
 *
 * Only an active job may describe itself with its current step. A finished job keeps the label of
 * its last step in the payload, and showing it made a card claim "Removing background" long after
 * the job had ended — with no progress indicator to contradict it.
 */
internal fun activeJobStepLabel(job: AiJob?): String? =
    job?.takeIf { it.status in ACTIVE_JOB_STATUSES }
        ?.progress
        ?.stepLabel
        ?.takeIf { it.isNotBlank() }

/**
 * Whether the card should render the raw failure message as an extra detail line.
 *
 * The status line already carries the failure message for drafts that need attention, so the extra
 * line is only useful when it adds information the status line does not already show.
 */
internal fun shouldShowFailureDetail(
    statusLabel: String,
    failureMessage: String?,
    progressMode: AiJobProgressMode
): Boolean {
    if (progressMode != AiJobProgressMode.NONE) return false
    val message = failureMessage?.takeIf { it.isNotBlank() } ?: return false
    return message.trim() != statusLabel.trim()
}
