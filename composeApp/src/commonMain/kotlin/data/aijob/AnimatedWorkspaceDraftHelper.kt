package data.aijob

import data.storage.StickerFileStorage
import domain.model.AnimatedStickerSpec
import domain.model.DecodedFrame
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AnimatedFrameSnapshot
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftContext
import domain.model.aijob.WorkspaceDraftKind
import domain.repository.WorkspaceDraftRepository
import kotlinx.serialization.encodeToString
import presentation.aijob.WorkspaceDraftFactory
import presentation.animatededitor.AnimatedEditorState
import presentation.animatededitor.DecorationApplyScope
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AnimatedWorkspaceDraftHelper(
    private val fileStorage: StickerFileStorage,
    private val draftRepository: WorkspaceDraftRepository
) {
    suspend fun createFromDecodedFrames(
        videoPath: String,
        spec: AnimatedStickerSpec,
        frames: List<DecodedFrame>,
        packId: String
    ): WorkspaceDraft {
        val draftId = Uuid.random().toString()
        val snapshots = frames.mapIndexed { index, frame ->
            val fileName = "anim_draft_${draftId}_$index.png"
            val path = fileStorage.saveBytes(frame.bytes, fileName)
            AnimatedFrameSnapshot(filePath = path, durationMs = frame.durationMs)
        }
        val context = WorkspaceDraftContext(
            videoPath = videoPath,
            animatedSpecJson = AiJobJson.codec.encodeToString(spec),
            animatedFrameSnapshots = snapshots,
            tempFilePaths = snapshots.map { it.filePath }
        )
        val packIdArg = packId.takeIf { it.isNotBlank() }
        val originRoute = buildString {
            append("animatedEditor/$draftId")
            if (!packIdArg.isNullOrBlank()) append("?packId=$packIdArg")
        }
        val draft = WorkspaceDraftFactory.create(
            kind = WorkspaceDraftKind.ANIMATED_EDITOR,
            origin = AiJobOrigin.ANIMATED_EDITOR,
            displayTitle = "Animated sticker",
            context = context,
            originRoute = originRoute,
            packId = packIdArg,
            id = draftId
        )
        draftRepository.upsert(draft)
        return draft
    }

    suspend fun loadFrames(draft: WorkspaceDraft): List<DecodedFrame>? {
        val context = WorkspaceDraftFactory.decodeContext(draft)
        if (context.animatedFrameSnapshots.isEmpty()) return null
        val frames = context.animatedFrameSnapshots.mapNotNull { snapshot ->
            val bytes = fileStorage.readBytesAtPath(snapshot.filePath) ?: return@mapNotNull null
            DecodedFrame(bytes = bytes, durationMs = snapshot.durationMs)
        }
        return frames.takeIf { it.size == context.animatedFrameSnapshots.size }
    }

    suspend fun syncEditorState(draftId: String, state: AnimatedEditorState) {
        val draft = draftRepository.getById(draftId) ?: return
        val previous = WorkspaceDraftFactory.decodeContext(draft)
        val updated = draft.copy(
            contextJson = AiJobJson.codec.encodeToString(
                previous.copy(
                    animatedBaseDecorations = state.baseDecorations,
                    animatedFrameDecorations = state.frameDecorations,
                    animatedEmojis = state.emojis,
                    animatedAccessibilityText = state.accessibilityText,
                    animatedApplyScope = state.applyScope.name,
                    animatedCurrentFrameIndex = state.currentFrameIndex
                )
            ),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        draftRepository.upsert(updated)
    }

    fun editorStateFromDraft(draft: WorkspaceDraft, frames: List<DecodedFrame>): AnimatedEditorState {
        val context = WorkspaceDraftFactory.decodeContext(draft)
        val applyScope = runCatching {
            DecorationApplyScope.valueOf(context.animatedApplyScope)
        }.getOrDefault(DecorationApplyScope.AllFrames)
        return AnimatedEditorState(
            draftId = draft.id,
            workspaceDraftId = draft.id,
            videoPath = context.videoPath.orEmpty(),
            frames = frames,
            currentFrameIndex = context.animatedCurrentFrameIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0)),
            baseDecorations = context.animatedBaseDecorations,
            frameDecorations = context.animatedFrameDecorations,
            applyScope = applyScope,
            emojis = context.animatedEmojis.ifEmpty { listOf("⭐") },
            accessibilityText = context.animatedAccessibilityText
        )
    }
}
