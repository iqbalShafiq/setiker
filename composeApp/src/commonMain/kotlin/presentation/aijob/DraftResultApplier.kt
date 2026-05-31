package presentation.aijob

import data.aijob.AiJobJson
import domain.model.aijob.AiJob
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.GeneratePackResult
import domain.model.aijob.GenerateStickersResult
import domain.model.aijob.GridSplitResult
import domain.model.aijob.ImproveStickersResult
import domain.model.aijob.RemoveBackgroundResult
import domain.model.aijob.VideoPackResult
import domain.model.aijob.WorkspaceDraft
import domain.model.aijob.toResolved
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import presentation.aijob.toDraftSticker
import presentation.createpack.CreatePackState
import presentation.createpack.GeneratedPreviewMode
import presentation.createpack.DraftSticker
import presentation.editor.EditorState
import presentation.home.HomeState
import presentation.videostickerpack.VideoStickerPackState

class DraftResultApplier(
    private val draftRepository: WorkspaceDraftRepository,
    private val jobRepository: AiJobRepository
) {
    fun observeDraft(draftId: String): Flow<WorkspaceDraft?> =
        draftRepository.observeById(draftId)

    fun observeJobCompletion(draftId: String): Flow<AiJob?> =
        combine(
            jobRepository.observeByDraft(draftId),
            draftRepository.observeById(draftId)
        ) { jobs, draft ->
            jobs.firstOrNull { it.status == AiJobStatus.COMPLETED }
                ?: draft?.lastCompletedJobId?.let { id -> jobs.firstOrNull { it.id == id } }
        }.distinctUntilChanged()

    fun applyToCreatePack(state: CreatePackState, draft: WorkspaceDraft, job: AiJob?): CreatePackState {
        if (job == null || job.status != AiJobStatus.COMPLETED) return state
        val context = WorkspaceDraftFactory.decodeContext(draft)
        var next = state.copy(
            workspaceDraftId = draft.id,
            isApiLoading = false,
            backgroundJobMessage = null
        )
        when (job.type) {
            AiJobType.GENERATE_STICKERS -> {
                val result = job.resultJson?.let {
                    AiJobJson.codec.decodeFromString<GenerateStickersResult>(it)
                } ?: return next
                next = next.copy(
                    generatedPreview = result.previews.map { it.toDraftSticker() },
                    selectedGeneratedPreview = result.previews.indices.toSet(),
                    generatedPreviewMode = GeneratedPreviewMode.AddToPack,
                    generatedResultsSheetVisible = true
                )
            }
            AiJobType.IMPROVE_STICKERS -> {
                val result = job.resultJson?.let {
                    AiJobJson.codec.decodeFromString<ImproveStickersResult>(it)
                } ?: return next
                next = next.copy(
                    generatedPreview = result.previews.map { it.toDraftSticker() },
                    selectedGeneratedPreview = result.previews.indices.toSet(),
                    generatedPreviewMode = if (result.replaceMode) {
                        GeneratedPreviewMode.ReplacePack
                    } else {
                        GeneratedPreviewMode.AddToPack
                    },
                    generatedResultsSheetVisible = true
                )
            }
            AiJobType.GRID_SPLIT -> {
                val result = job.resultJson?.let {
                    AiJobJson.codec.decodeFromString<GridSplitResult>(it)
                } ?: return next
                next = next.copy(
                    splitPreview = result.previews.map { it.toDraftSticker() },
                    selectedSplitPreview = result.previews.indices.toSet(),
                    gridSplitSheetPhase = presentation.createpack.GridSplitSheetPhase.Results
                )
            }
            else -> Unit
        }
        return next.mergeContext(context)
    }

    fun applyToEditor(state: EditorState, draft: WorkspaceDraft, job: AiJob?): EditorState {
        if (job == null || job.status != AiJobStatus.COMPLETED) return state
        val context = WorkspaceDraftFactory.decodeContext(draft)
        var next = state.copy(isApiLoading = false, isBackgroundRemoving = false, backgroundJobMessage = null)
        when (job.type) {
            AiJobType.GENERATE_STICKERS, AiJobType.IMPROVE_STICKERS -> {
                val result = job.resultJson?.let {
                    if (job.type == AiJobType.GENERATE_STICKERS) {
                        AiJobJson.codec.decodeFromString<GenerateStickersResult>(it)
                    } else {
                        AiJobJson.codec.decodeFromString<ImproveStickersResult>(it).let { improve ->
                            GenerateStickersResult(improve.previews)
                        }
                    }
                } ?: return next
                next = next.copy(
                    generatedPreview = result.previews.map { it.toDraftSticker() },
                    generatedResultsSheetVisible = true
                )
            }
            AiJobType.REMOVE_BACKGROUND -> {
                val result = job.resultJson?.let {
                    AiJobJson.codec.decodeFromString<RemoveBackgroundResult>(it)
                } ?: return next
                next = next.copy(
                    backgroundRemoverPreviewPath = result.outputPath,
                    isBackgroundRemoverSheetOpen = true
                )
            }
            else -> Unit
        }
        return next.copy(
            workspaceDraftId = draft.id,
            generatePrompt = context.prompt.ifBlank { next.generatePrompt },
            generateInputImage = context.inputImagePath ?: next.generateInputImage
        )
    }

    fun applyToVideoPack(state: VideoStickerPackState, draft: WorkspaceDraft, job: AiJob?): VideoStickerPackState {
        if (job == null || job.status != AiJobStatus.COMPLETED) return state
        val context = WorkspaceDraftFactory.decodeContext(draft)
        val planJson = job.resultJson?.let {
            AiJobJson.codec.decodeFromString<VideoPackResult>(it).videoPlanResultJson
        } ?: context.videoPlanResultJson
        val plan = planJson?.let { json ->
            runCatching {
                AiJobJson.codec.decodeFromString<domain.model.aijob.StoredVideoPackPlan>(json).toResolved()
            }.getOrNull()
        }
        return state.copy(
            workspaceDraftId = draft.id,
            isProcessing = false,
            processingStep = null,
            generatedPlan = plan,
            selectedStaticStickerKeys = context.selectedStaticKeys,
            selectedAnimatedStickerKeys = context.selectedAnimatedKeys,
            candidates = context.candidatePaths.map { path ->
                domain.model.VideoFrameCandidate(
                    filePath = path,
                    timestampMs = 0L,
                    sharpnessScore = 0.0,
                    brightnessScore = 0.0,
                    differenceScore = 0.0
                )
            },
            candidateGrids = context.candidateGridPaths.map { path ->
                domain.model.CandidateGridImage(filePath = path, frameCount = 16)
            },
            candidateManifest = context.candidateManifest,
            prompt = context.prompt,
            packName = context.packName,
            publisher = context.publisher
        )
    }

    fun applyGeneratePackCompletion(state: HomeState, draft: WorkspaceDraft, job: AiJob?): String? {
        if (job == null || job.status != AiJobStatus.COMPLETED || job.type != AiJobType.GENERATE_PACK) return null
        return job.resultJson?.let {
            AiJobJson.codec.decodeFromString<GeneratePackResult>(it).packId
        } ?: WorkspaceDraftFactory.decodeContext(draft).resultPackId
    }

    private fun CreatePackState.mergeContext(context: domain.model.aijob.WorkspaceDraftContext): CreatePackState =
        copy(
            name = context.packName.ifBlank { name },
            publisher = context.publisher.ifBlank { publisher },
            visibility = context.visibility,
            trayImagePath = context.trayImagePath.ifBlank { trayImagePath },
            stickers = context.stickers.map { it.toDraftSticker() }.ifEmpty { stickers },
            generatePrompt = context.prompt,
            generateInputImage = context.inputImagePath,
            gridSplitSourcePath = context.gridSplitSourcePath,
            gridLayout = context.gridLayout ?: gridLayout,
            generatedPreview = context.generatedPreview.map { it.toDraftSticker() }
                .ifEmpty { generatedPreview },
            selectedGeneratedPreview = if (context.generatedPreview.isNotEmpty()) {
                context.selectedGeneratedPreview
            } else {
                selectedGeneratedPreview
            },
            generatedPreviewMode = if (context.generatedPreview.isNotEmpty()) {
                context.generatedPreviewMode.toGeneratedPreviewMode()
            } else {
                generatedPreviewMode
            },
            splitPreview = context.splitPreview.map { it.toDraftSticker() }.ifEmpty { splitPreview },
            selectedSplitPreview = if (context.splitPreview.isNotEmpty()) {
                context.selectedSplitPreview
            } else {
                selectedSplitPreview
            }
        )

    private fun String.toGeneratedPreviewMode(): GeneratedPreviewMode =
        when (this) {
            "ReplaceInPack", "ReplacePack" -> GeneratedPreviewMode.ReplacePack
            else -> GeneratedPreviewMode.AddToPack
        }
}
