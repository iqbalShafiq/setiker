package presentation.aijob

import data.aijob.AiJobJson
import data.aijob.AiJobManager
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AiJobType
import domain.model.aijob.AnimatedEncodePayload
import domain.model.aijob.GeneratePackPayload
import domain.model.aijob.GenerateStickersPayload
import domain.model.aijob.GridSplitPayload
import domain.model.aijob.ImproveStickersPayload
import domain.model.aijob.RemoveBackgroundPayload
import domain.model.aijob.VideoPackPayload
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftKind
import kotlinx.serialization.encodeToString

class AiJobEnqueueHelper(
    private val aiJobManager: AiJobManager
) {
    suspend fun enqueueGeneratePack(draft: WorkspaceDraft, payload: GeneratePackPayload) =
        aiJobManager.enqueue(
            draft = draft,
            type = AiJobType.GENERATE_PACK,
            origin = AiJobOrigin.HOME_SHEET,
            payloadJson = AiJobJson.codec.encodeToString(payload),
            requiresNetwork = true
        )

    suspend fun enqueueGenerateStickers(
        draft: WorkspaceDraft,
        origin: AiJobOrigin,
        payload: GenerateStickersPayload
    ) = aiJobManager.enqueue(
        draft = draft,
        type = AiJobType.GENERATE_STICKERS,
        origin = origin,
        payloadJson = AiJobJson.codec.encodeToString(payload),
        requiresNetwork = true
    )

    suspend fun enqueueImproveStickers(
        draft: WorkspaceDraft,
        origin: AiJobOrigin,
        payload: ImproveStickersPayload
    ) = aiJobManager.enqueue(
        draft = draft,
        type = AiJobType.IMPROVE_STICKERS,
        origin = origin,
        payloadJson = AiJobJson.codec.encodeToString(payload),
        requiresNetwork = true
    )

    suspend fun enqueueGridSplit(
        draft: WorkspaceDraft,
        payload: GridSplitPayload
    ) = aiJobManager.enqueue(
        draft = draft,
        type = AiJobType.GRID_SPLIT,
        origin = AiJobOrigin.CREATE_PACK,
        payloadJson = AiJobJson.codec.encodeToString(payload),
        requiresNetwork = false
    )

    suspend fun enqueueRemoveBackground(
        draft: WorkspaceDraft,
        payload: RemoveBackgroundPayload
    ) = aiJobManager.enqueue(
        draft = draft,
        type = AiJobType.REMOVE_BACKGROUND,
        origin = AiJobOrigin.EDITOR,
        payloadJson = AiJobJson.codec.encodeToString(payload),
        requiresNetwork = false
    )

    suspend fun enqueueVideoPack(
        draft: WorkspaceDraft,
        payload: VideoPackPayload
    ) = aiJobManager.enqueue(
        draft = draft,
        type = AiJobType.VIDEO_PACK,
        origin = AiJobOrigin.VIDEO_STICKER_PACK,
        payloadJson = AiJobJson.codec.encodeToString(payload),
        requiresNetwork = true
    )

    suspend fun enqueueAnimatedEncode(
        draft: WorkspaceDraft,
        payload: AnimatedEncodePayload
    ) = aiJobManager.enqueue(
        draft = draft,
        type = AiJobType.ANIMATED_ENCODE,
        origin = AiJobOrigin.ANIMATED_EDITOR,
        payloadJson = AiJobJson.codec.encodeToString(payload),
        requiresNetwork = false
    )
}
