package presentation.aijob

import domain.model.aijob.AiJob
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.WorkspaceDraft
import data.aijob.AiJobJson
import domain.model.aijob.GeneratePackResult
import domain.model.aijob.WorkspaceDraftStatus
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import presentation.navigation.PathEncoder

/**
 * Resolves where to navigate when the user taps an AI job / workspace draft notification.
 * Resolution uses the latest draft + job state at tap time (not when the notification was posted).
 */
object WorkspaceDraftNavigation {

    data class Target(
        val route: String,
        val navigateToAiJobsFirst: Boolean = false
    )

    suspend fun resolveNotificationTarget(
        draftRepository: WorkspaceDraftRepository,
        jobRepository: AiJobRepository,
        draftId: String?,
        jobId: String?
    ): Target {
        val resolvedDraftId = draftId?.takeIf { it.isNotBlank() }
            ?: jobId?.let { jobRepository.getById(it)?.workspaceDraftId }
            ?: return Target(route = "aiJobs")
        val draft = draftRepository.getById(resolvedDraftId)
            ?: return Target(route = "aiJobs")
        val job = resolveJob(jobRepository, draft, jobId)
        return resolve(draft, job)
    }

    fun routeForDraft(draftId: String, originRoute: String?): String =
        buildRouteFromOrigin(draftId, originRoute)

    private suspend fun resolveJob(
        jobRepository: AiJobRepository,
        draft: WorkspaceDraft,
        preferredJobId: String?
    ): AiJob? {
        preferredJobId?.let { jobRepository.getById(it) }?.let { return it }
        draft.lastJobId?.let { jobRepository.getById(it) }?.let { return it }
        draft.lastCompletedJobId?.let { jobRepository.getById(it) }?.let { return it }
        draft.lastFailedJobId?.let { jobRepository.getById(it) }?.let { return it }
        return null
    }

    private fun resolve(draft: WorkspaceDraft, job: AiJob?): Target {
        val context = WorkspaceDraftFactory.decodeContext(draft)

        when (job?.status) {
            AiJobStatus.QUEUED,
            AiJobStatus.RUNNING,
            AiJobStatus.WAITING_FOR_NETWORK,
            AiJobStatus.CHECKPOINTED,
            AiJobStatus.CANCEL_REQUESTED -> {
                return Target(
                    route = buildRouteFromOrigin(draft.id, draft.originRoute),
                    navigateToAiJobsFirst = false
                )
            }
            AiJobStatus.COMPLETED -> {
                if (job.type == AiJobType.GENERATE_PACK) {
                    val packId = job.resultJson?.let {
                        AiJobJson.codec.decodeFromString<GeneratePackResult>(it).packId
                    } ?: context.resultPackId
                    if (!packId.isNullOrBlank()) {
                        return Target(route = "packDetail/$packId")
                    }
                }
                if (draft.status == WorkspaceDraftStatus.READY_TO_REVIEW ||
                    draft.status == WorkspaceDraftStatus.APPLIED
                ) {
                    return Target(route = buildRouteFromOrigin(draft.id, draft.originRoute))
                }
                return Target(route = "aiJobs")
            }
            AiJobStatus.FAILED_RETRYABLE,
            AiJobStatus.FAILED_FINAL -> {
                return Target(route = "aiJobs")
            }
            AiJobStatus.CANCELLED -> {
                return Target(route = "aiJobs")
            }
            else -> Unit
        }

        return when (draft.status) {
            WorkspaceDraftStatus.NEEDS_ATTENTION,
            WorkspaceDraftStatus.CANCELLED -> Target(route = "aiJobs")
            WorkspaceDraftStatus.READY_TO_REVIEW,
            WorkspaceDraftStatus.APPLIED -> Target(route = buildRouteFromOrigin(draft.id, draft.originRoute))
            WorkspaceDraftStatus.PROCESSING,
            WorkspaceDraftStatus.ACTIVE -> Target(
                route = buildRouteFromOrigin(draft.id, draft.originRoute),
                navigateToAiJobsFirst = true
            )
        }
    }

    private fun buildRouteFromOrigin(draftId: String, originRoute: String?): String {
        val route = originRoute.orEmpty()
        return when {
            route.startsWith("editor") ->
                "editor?packId=&stickerIndex=-1&workspaceDraftId=$draftId"
            route.startsWith("videoStickerPack/") -> {
                val videoPath = route.removePrefix("videoStickerPack/")
                "videoStickerPack/${PathEncoder.encode(videoPath)}"
            }
            route.startsWith("animatedEditor/") -> {
                val packArg = route.substringAfter("packId=", "").substringBefore("&")
                if (packArg.isBlank()) "animatedEditor/$draftId"
                else "animatedEditor/$draftId?packId=${PathEncoder.encode(packArg)}"
            }
            route.contains("createPack") -> {
                val packArg = route.substringAfter("packId=", "")
                if (packArg.isBlank() || packArg == "{packId}") {
                    "createPack?workspaceDraftId=$draftId"
                } else {
                    "createPack?packId=$packArg&workspaceDraftId=$draftId"
                }
            }
            draftId.isNotBlank() -> "createPack?workspaceDraftId=$draftId"
            else -> "aiJobs"
        }
    }
}
