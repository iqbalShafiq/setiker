package data.aijob

import data.local.entity.AiJobEntity
import data.local.entity.WorkspaceDraftEntity
import domain.model.aijob.AiJob
import domain.model.aijob.AiJobFailureKind
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AiJobProgress
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftKind
import domain.model.aijob.WorkspaceDraftStatus

fun AiJobEntity.toDomain(): AiJob = AiJob(
    id = id,
    workspaceDraftId = workspaceDraftId,
    type = AiJobType.valueOf(type),
    status = AiJobStatus.valueOf(status),
    origin = AiJobOrigin.valueOf(origin),
    payloadJson = payloadJson,
    resultJson = resultJson,
    checkpointJson = checkpointJson,
    progress = progressStepKey?.let {
        AiJobProgress(
            stepKey = it,
            stepLabel = progressStepLabel.orEmpty(),
            fraction = progressFraction
        )
    },
    attemptCount = attemptCount,
    attemptGroupId = attemptGroupId,
    parentJobId = parentJobId,
    failureKind = AiJobFailureKind.valueOf(failureKind),
    failureMessage = failureMessage,
    requiresNetwork = requiresNetwork,
    quotaReservationId = quotaReservationId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAttemptAt = lastAttemptAt,
    nextRetryAt = nextRetryAt,
    completedAt = completedAt
)

fun AiJob.toEntity(): AiJobEntity = AiJobEntity(
    id = id,
    workspaceDraftId = workspaceDraftId,
    type = type.name,
    status = status.name,
    origin = origin.name,
    payloadJson = payloadJson,
    resultJson = resultJson,
    checkpointJson = checkpointJson,
    progressStepKey = progress?.stepKey,
    progressStepLabel = progress?.stepLabel,
    progressFraction = progress?.fraction ?: 0f,
    attemptCount = attemptCount,
    attemptGroupId = attemptGroupId,
    parentJobId = parentJobId,
    failureKind = failureKind.name,
    failureMessage = failureMessage,
    requiresNetwork = requiresNetwork,
    quotaReservationId = quotaReservationId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAttemptAt = lastAttemptAt,
    nextRetryAt = nextRetryAt,
    completedAt = completedAt
)

fun WorkspaceDraftEntity.toDomain(): WorkspaceDraft = WorkspaceDraft(
    id = id,
    kind = WorkspaceDraftKind.valueOf(kind),
    status = WorkspaceDraftStatus.valueOf(status),
    origin = AiJobOrigin.valueOf(origin),
    originRoute = originRoute,
    packId = packId,
    stickerIndex = stickerIndex,
    displayTitle = displayTitle,
    contextJson = contextJson,
    lastJobId = lastJobId,
    lastCompletedJobId = lastCompletedJobId,
    lastFailedJobId = lastFailedJobId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkspaceDraft.toEntity(): WorkspaceDraftEntity = WorkspaceDraftEntity(
    id = id,
    kind = kind.name,
    status = status.name,
    origin = origin.name,
    originRoute = originRoute,
    packId = packId,
    stickerIndex = stickerIndex,
    displayTitle = displayTitle,
    contextJson = contextJson,
    lastJobId = lastJobId,
    lastCompletedJobId = lastCompletedJobId,
    lastFailedJobId = lastFailedJobId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
