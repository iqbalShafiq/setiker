package presentation.aijob

import data.aijob.AiJobJson
import data.aijob.AiJobManager
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.aijob.AiJob
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.DraftStickerSnapshot
import domain.model.aijob.ImproveStickersResult
import domain.model.aijob.VideoPackResult
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.WorkspaceDraftContext
import domain.model.aijob.toStored
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import presentation.videostickerpack.selectionKey
import presentation.aijob.WorkspaceDraftFactory
import kotlin.time.Clock

object ViewModelAiJobTestSupport {
    fun manager(
        onUpsertDraft: (WorkspaceDraft) -> Unit = {},
    ): AiJobManager = mockk(relaxed = true) {
        every { observeActiveJobCount() } returns flowOf(0)
        coEvery { upsertDraft(any()) } coAnswers {
            onUpsertDraft(firstArg())
        }
        coEvery { enqueue(any(), any(), any(), any(), any(), any(), any()) } answers {
            AiJob(
                id = "job_test",
                workspaceDraftId = firstArg<WorkspaceDraft>().id,
                type = arg(1),
                status = AiJobStatus.QUEUED,
                origin = arg(2),
                payloadJson = arg(3),
                attemptGroupId = "group_test",
                requiresNetwork = arg(4),
                createdAt = now(),
                updatedAt = now()
            )
        }
    }

    fun enqueueHelper(manager: AiJobManager = manager()): AiJobEnqueueHelper =
        AiJobEnqueueHelper(manager)

    fun createPackDependencies(
        jobsFlow: MutableStateFlow<List<AiJob>> = MutableStateFlow(emptyList()),
        draftsFlow: MutableStateFlow<WorkspaceDraft?> = MutableStateFlow(null),
    ): CreatePackAiDeps {
        val jobRepo = mockk<AiJobRepository> {
            every { observeByDraft(any()) } returns jobsFlow
        }
        val draftRepo = mockk<WorkspaceDraftRepository> {
            every { observeById(any()) } answers {
                val id = firstArg<String>()
                draftsFlow.map { draft -> if (draft?.id == id) draft else null }
            }
        }
        val manager = manager { draft ->
            draftsFlow.value = draft
        }
        every { manager.observeJobs() } returns jobsFlow
        every { manager.observeDrafts() } returns draftsFlow.map { listOfNotNull(it) }
        return CreatePackAiDeps(
            manager = manager,
            enqueueHelper = enqueueHelper(manager),
            draftResultApplier = DraftResultApplier(draftRepo, jobRepo),
            jobRepository = jobRepo,
            jobsFlow = jobsFlow,
            draftsFlow = draftsFlow
        )
    }

    fun editorDependencies(
        jobsFlow: MutableStateFlow<List<AiJob>> = MutableStateFlow(emptyList()),
        draftsFlow: MutableStateFlow<WorkspaceDraft?> = MutableStateFlow(null),
    ): EditorAiDeps {
        val jobRepo = mockk<AiJobRepository> {
            every { observeByDraft(any()) } returns jobsFlow
        }
        val draftRepo = mockk<WorkspaceDraftRepository> {
            every { observeById(any()) } answers {
                val id = firstArg<String>()
                draftsFlow.map { draft -> if (draft?.id == id) draft else null }
            }
        }
        val manager = manager { draft ->
            draftsFlow.value = draft
        }
        every { manager.observeJobs() } returns jobsFlow
        every { manager.observeDrafts() } returns draftsFlow.map { listOfNotNull(it) }
        return EditorAiDeps(
            manager = manager,
            enqueueHelper = enqueueHelper(manager),
            draftResultApplier = DraftResultApplier(draftRepo, jobRepo),
            jobsFlow = jobsFlow,
            draftsFlow = draftsFlow
        )
    }

    fun videoDependencies(
        plan: ResolvedVideoStickerPackPlan,
        jobsFlow: MutableStateFlow<List<AiJob>> = MutableStateFlow(emptyList()),
        draftsFlow: MutableStateFlow<WorkspaceDraft?> = MutableStateFlow(null),
    ): VideoAiDeps {
        val jobRepo = mockk<AiJobRepository> {
            every { observeByDraft(any()) } returns jobsFlow
        }
        val draftRepo = mockk<WorkspaceDraftRepository> {
            every { observeById(any()) } answers {
                val id = firstArg<String>()
                draftsFlow.map { draft -> if (draft?.id == id) draft else null }
            }
        }
        val manager = manager { draft ->
            draftsFlow.value = draft
        }
        every { manager.observeJobs() } returns jobsFlow
        every { manager.observeDrafts() } returns draftsFlow.map { listOfNotNull(it) }
        return VideoAiDeps(
            manager = manager,
            enqueueHelper = enqueueHelper(manager),
            draftResultApplier = DraftResultApplier(draftRepo, jobRepo),
            jobsFlow = jobsFlow,
            draftsFlow = draftsFlow,
            plan = plan
        )
    }

    fun completeImproveJob(
        draftId: String,
        previews: List<DraftStickerSnapshot>,
        replaceMode: Boolean = true
    ): AiJob = AiJob(
        id = "job_improve",
        workspaceDraftId = draftId,
        type = AiJobType.IMPROVE_STICKERS,
        status = AiJobStatus.COMPLETED,
        origin = AiJobOrigin.CREATE_PACK,
        payloadJson = "{}",
        resultJson = AiJobJson.codec.encodeToString(
            ImproveStickersResult(previews = previews, replaceMode = replaceMode)
        ),
        attemptGroupId = "group",
        createdAt = now(),
        updatedAt = now()
    )

    fun completeVideoPackJob(
        draftId: String,
        plan: ResolvedVideoStickerPackPlan
    ): AiJob = AiJob(
        id = "job_video",
        workspaceDraftId = draftId,
        type = AiJobType.VIDEO_PACK,
        status = AiJobStatus.COMPLETED,
        origin = AiJobOrigin.VIDEO_STICKER_PACK,
        payloadJson = "{}",
        resultJson = AiJobJson.codec.encodeToString(
            VideoPackResult(
                videoPlanResultJson = AiJobJson.codec.encodeToString(plan.toStored())
            )
        ),
        attemptGroupId = "group",
        createdAt = now(),
        updatedAt = now()
    )

    fun applyVideoPlanSelection(state: presentation.videostickerpack.VideoStickerPackState, plan: ResolvedVideoStickerPackPlan) =
        state.copy(
            generatedPlan = plan,
            isProcessing = false,
            processingStep = null,
            selectedStaticStickerKeys = plan.staticStickers.map { it.selectionKey() }.toSet(),
            selectedAnimatedStickerKeys = plan.animatedStickers.mapIndexed { index, sticker ->
                sticker.selectionKey(index)
            }.toSet()
        )

    private fun now() = Clock.System.now().toEpochMilliseconds()
}

data class CreatePackAiDeps(
    val manager: AiJobManager,
    val enqueueHelper: AiJobEnqueueHelper,
    val draftResultApplier: DraftResultApplier,
    val jobRepository: AiJobRepository,
    val jobsFlow: MutableStateFlow<List<AiJob>>,
    val draftsFlow: MutableStateFlow<WorkspaceDraft?>
)

data class EditorAiDeps(
    val manager: AiJobManager,
    val enqueueHelper: AiJobEnqueueHelper,
    val draftResultApplier: DraftResultApplier,
    val jobsFlow: MutableStateFlow<List<AiJob>>,
    val draftsFlow: MutableStateFlow<WorkspaceDraft?>
)

data class VideoAiDeps(
    val manager: AiJobManager,
    val enqueueHelper: AiJobEnqueueHelper,
    val draftResultApplier: DraftResultApplier,
    val jobsFlow: MutableStateFlow<List<AiJob>>,
    val draftsFlow: MutableStateFlow<WorkspaceDraft?>,
    val plan: ResolvedVideoStickerPackPlan
) {
    fun emitVideoCompletion() {
        val draft = draftsFlow.value ?: return
        val planJson = AiJobJson.codec.encodeToString(plan.toStored())
        val context = WorkspaceDraftFactory.decodeContext(draft).copy(
            videoPlanResultJson = planJson,
            selectedStaticKeys = plan.staticStickers.map { it.selectionKey() }.toSet(),
            selectedAnimatedKeys = plan.animatedStickers.mapIndexed { index, sticker ->
                sticker.selectionKey(index)
            }.toSet()
        )
        draftsFlow.value = draft.copy(
            contextJson = AiJobJson.codec.encodeToString(context)
        )
        jobsFlow.value = listOf(ViewModelAiJobTestSupport.completeVideoPackJob(draft.id, plan))
    }
}
