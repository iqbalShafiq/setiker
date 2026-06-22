package presentation.aijob

import data.aijob.AiJobJson
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftContext
import domain.model.aijob.WorkspaceDraftKind
import domain.model.aijob.WorkspaceDraftStatus
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object WorkspaceDraftFactory {
    fun create(
        kind: WorkspaceDraftKind,
        origin: AiJobOrigin,
        displayTitle: String,
        context: WorkspaceDraftContext,
        originRoute: String? = null,
        packId: String? = null,
        stickerIndex: Int? = null,
        id: String = Uuid.random().toString()
    ): WorkspaceDraft {
        val now = Clock.System.now().toEpochMilliseconds()
        return WorkspaceDraft(
            id = id,
            kind = kind,
            status = WorkspaceDraftStatus.ACTIVE,
            origin = origin,
            originRoute = originRoute,
            packId = packId,
            stickerIndex = stickerIndex,
            displayTitle = displayTitle,
            contextJson = AiJobJson.codec.encodeToString(context),
            createdAt = now,
            updatedAt = now
        )
    }

    fun decodeContext(draft: WorkspaceDraft): WorkspaceDraftContext =
        AiJobJson.codec.decodeFromString(draft.contextJson)
}
