package data.aijob

import data.remote.GridSplitOnDeviceProgressUpdate
import data.remote.GridSplitProgressPhase
import data.remote.StickerApiRepository
import data.remote.gridSplitFraction
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.model.VideoFrameCandidate
import domain.model.aijob.AiJob
import domain.model.aijob.AiJobFailureKind
import domain.model.aijob.AiJobProgress
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.AnimatedEncodePayload
import domain.model.aijob.AnimatedEncodeResult
import domain.model.aijob.GeneratePackPayload
import domain.model.aijob.GeneratePackResult
import domain.model.aijob.GenerateStickersPayload
import domain.model.aijob.GenerateStickersResult
import domain.model.aijob.GridSplitPayload
import domain.model.aijob.GridSplitResult
import domain.model.aijob.ImproveStickersPayload
import domain.model.aijob.ImproveStickersResult
import domain.model.aijob.RemoveBackgroundPayload
import domain.model.aijob.RemoveBackgroundResult
import domain.model.aijob.VideoPackCheckpoint
import domain.model.aijob.VideoPackPayload
import domain.model.aijob.VideoPackResult
import domain.model.aijob.WorkspaceDraftContext
import domain.model.aijob.WorkspaceDraftStatus
import domain.model.aijob.toStored
import domain.repository.AiJobRepository
import domain.repository.StickerRepository
import domain.repository.WorkspaceDraftRepository
import domain.util.VideoStickerPackPlanner
import data.util.OnDeviceImageProcessor
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import presentation.aijob.toSnapshot
import presentation.common.PackIdentifierSanitizer
import presentation.videostickerpack.selectionKey
import kotlin.random.Random

class AiJobRunner(
    private val jobRepository: AiJobRepository,
    private val draftRepository: WorkspaceDraftRepository,
    private val apiRepository: StickerApiRepository,
    private val onDeviceImageProcessor: OnDeviceImageProcessor,
    private val fileStorage: StickerFileStorage,
    private val extractor: VideoFrameCandidateExtractor,
    private val gridComposer: CandidateGridComposer,
    private val draftSaver: StickerPackDraftSaver,
    private val stickerRepository: StickerRepository,
    private val jobManager: AiJobManager,
    private val animatedDraftHelper: AnimatedWorkspaceDraftHelper
) {
    suspend fun runClaimedJob(job: AiJob): Boolean {
        if (isCancelled(job.id)) return false
        return try {
            when (job.type) {
                AiJobType.GENERATE_STICKERS -> runGenerateStickers(job)
                AiJobType.GENERATE_PACK -> runGeneratePack(job)
                AiJobType.IMPROVE_STICKERS -> runImproveStickers(job)
                AiJobType.GRID_SPLIT -> runGridSplit(job)
                AiJobType.REMOVE_BACKGROUND -> runRemoveBackground(job)
                AiJobType.VIDEO_PACK -> runVideoPack(job)
                AiJobType.ANIMATED_ENCODE -> runAnimatedEncode(job)
            }
            true
        } catch (throwable: Throwable) {
            handleFailure(job, throwable)
            false
        }
    }

    private suspend fun runGenerateStickers(job: AiJob) {
        val payload = AiJobJson.codec.decodeFromString<GenerateStickersPayload>(job.payloadJson)
        updateProgress(job.id, "api_generate", "Generating stickers", 0.2f)
        if (!payload.inputImagePath.isNullOrBlank() && !SourcePathValidator.exists(payload.inputImagePath)) {
            throw IllegalStateException("Input image file not found")
        }
        val generated = apiRepository.generateStickers(payload.prompt, payload.inputImagePath)
        val result = GenerateStickersResult(
            previews = generated.map { file ->
                presentation.createpack.DraftSticker(
                    imagePath = file.localPath,
                    decorations = file.decorations
                ).toSnapshot()
            }
        )
        completeJob(job, AiJobJson.codec.encodeToString(result))
        mergeDraftContext(job.workspaceDraftId) { context ->
            context.copy(
                generatedPreview = result.previews,
                selectedGeneratedPreview = result.previews.indices.toSet(),
                generatedPreviewMode = "AddToPack",
                lastCompletedStep = "generate_stickers"
            )
        }
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.READY_TO_REVIEW)
    }

    private suspend fun runGeneratePack(job: AiJob) {
        val payload = AiJobJson.codec.decodeFromString<GeneratePackPayload>(job.payloadJson)
        updateProgress(job.id, "api_generate_pack", "Menghubungi server AI…", 0.15f)
        val rawGridPath = coroutineScope {
            val waiter = launch {
                var pulse = 0.15f
                while (isActive) {
                    delay(2_000)
                    pulse = (pulse + 0.04f).coerceAtMost(0.45f)
                    updateProgress(
                        job.id,
                        "api_generate_pack",
                        "Menunggu respons server AI…",
                        pulse
                    )
                }
            }
            try {
                apiRepository.fetchGeneratePackGridPath(
                    prompt = payload.prompt,
                    layout = payload.layout,
                    inputImagePath = payload.inputImagePath
                )
            } finally {
                waiter.cancel()
            }
        } ?: throw IllegalStateException("Server tidak mengembalikan gambar pack")
        val generated = splitGridWithProgress(job.id, rawGridPath, payload.layout)
        if (generated.isEmpty()) {
            throw IllegalStateException("Server tidak mengembalikan gambar pack")
        }
        updateProgress(job.id, "save_pack", "Menyimpan pack", 0.85f)
        val trayImagePath = generated.firstOrNull()?.localPath
            ?: throw IllegalStateException("No generated sticker returned")
        val identifier = PackIdentifierSanitizer.sanitize(
            rawName = payload.packName,
            suffix = Random.nextInt(1000, 9999)
        )
        val pack = draftSaver.buildDraftPack(
            StickerDraftInput(
                identifier = identifier,
                name = payload.packName,
                publisher = payload.publisher,
                visibility = "PRIVATE",
                trayImagePath = trayImagePath,
                stickers = generated.map { file ->
                    StickerDraftInput.StickerInput(
                        imagePath = file.localPath,
                        decorations = file.decorations
                    )
                }
            )
        )
        stickerRepository.savePack(pack)
        val result = GeneratePackResult(packId = pack.identifier)
        completeJob(job, AiJobJson.codec.encodeToString(result))
        mergeDraftContext(job.workspaceDraftId) { context ->
            context.copy(
                resultPackId = pack.identifier,
                homeAutoSaveOnComplete = true,
                lastCompletedStep = "generate_pack"
            )
        }
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.APPLIED)
    }

    private suspend fun runImproveStickers(job: AiJob) {
        val payload = AiJobJson.codec.decodeFromString<ImproveStickersPayload>(job.payloadJson)
        updateProgress(job.id, "api_improve", "Improving stickers", 0.3f)
        payload.imagePaths.forEach { path ->
            if (!SourcePathValidator.exists(path)) throw IllegalStateException("Sticker file not found: $path")
        }
        val improved = apiRepository.improve(payload.imagePaths)
        val result = ImproveStickersResult(
            previews = improved.map { file ->
                presentation.createpack.DraftSticker(
                    imagePath = file.localPath,
                    decorations = file.decorations
                ).toSnapshot()
            },
            replaceMode = payload.imagePaths.size > 1
        )
        completeJob(job, AiJobJson.codec.encodeToString(result))
        mergeDraftContext(job.workspaceDraftId) { context ->
            context.copy(
                generatedPreview = result.previews,
                selectedGeneratedPreview = result.previews.indices.toSet(),
                generatedPreviewMode = if (result.replaceMode) "ReplaceInPack" else "AddToPack",
                lastCompletedStep = "improve_stickers"
            )
        }
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.READY_TO_REVIEW)
    }

    private suspend fun runGridSplit(job: AiJob) {
        val payload = AiJobJson.codec.decodeFromString<GridSplitPayload>(job.payloadJson)
        if (!SourcePathValidator.exists(payload.imagePath)) {
            throw IllegalStateException("Grid source file not found")
        }
        val splitImages = splitGridWithProgress(job.id, payload.imagePath, payload.layout)
        val result = GridSplitResult(
            previews = splitImages.map { file ->
                presentation.createpack.DraftSticker(
                    imagePath = file.localPath,
                    decorations = file.decorations
                ).toSnapshot()
            }
        )
        completeJob(job, AiJobJson.codec.encodeToString(result))
        mergeDraftContext(job.workspaceDraftId) { context ->
            context.copy(
                splitPreview = result.previews,
                selectedSplitPreview = result.previews.indices.toSet(),
                lastCompletedStep = "grid_split"
            )
        }
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.READY_TO_REVIEW)
    }

    private suspend fun runRemoveBackground(job: AiJob) {
        val payload = AiJobJson.codec.decodeFromString<RemoveBackgroundPayload>(job.payloadJson)
        if (!SourcePathValidator.exists(payload.imagePath)) {
            throw IllegalStateException("Source image file not found")
        }
        updateProgress(job.id, "remove_bg", "Removing background", 0.4f)
        val outputPath = onDeviceImageProcessor.removeBackground(payload.imagePath)
        val result = RemoveBackgroundResult(outputPath = outputPath)
        completeJob(job, AiJobJson.codec.encodeToString(result))
        mergeDraftContext(job.workspaceDraftId) { context ->
            context.copy(
                backgroundPreviewPath = outputPath,
                lastCompletedStep = "remove_background"
            )
        }
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.READY_TO_REVIEW)
    }

    private suspend fun runVideoPack(job: AiJob) {
        val payload = AiJobJson.codec.decodeFromString<VideoPackPayload>(job.payloadJson)
        val checkpoint = job.checkpointJson?.let {
            AiJobJson.codec.decodeFromString<VideoPackCheckpoint>(it)
        } ?: VideoPackCheckpoint()

        var candidatePaths = checkpoint.candidatePaths
        var gridPaths = checkpoint.candidateGridPaths
        var manifest = checkpoint.candidateManifest

        if (candidatePaths.isEmpty() || gridPaths.isEmpty() || manifest.isEmpty()) {
            updateProgress(job.id, "extract_frames", "Finding frames", 0.15f)
            val extracted = extractor.extractCandidates(
                videoPath = payload.videoPath,
                startMs = payload.selectedStartMs,
                endMs = payload.selectedEndMs,
                onProgress = { _, _ -> }
            ).take(VideoStickerPackPlanner.MAX_CANDIDATES)
            if (extracted.isEmpty()) error("No usable frames extracted")
            candidatePaths = extracted.map { it.filePath }
            updateProgress(job.id, "compose_grids", "Building grids", 0.55f)
            val composed = gridComposer.composeGrids(extracted.map { it.filePath })
            gridPaths = composed.map { it.filePath }
            manifest = VideoStickerPackPlanner.buildCandidateManifest(extracted, composed)
            saveCheckpoint(
                job,
                VideoPackCheckpoint(
                    candidatePaths = candidatePaths,
                    candidateGridPaths = gridPaths,
                    candidateManifest = manifest
                )
            )
            mergeDraftContext(job.workspaceDraftId) { context ->
                context.copy(
                    candidatePaths = candidatePaths,
                    candidateGridPaths = gridPaths,
                    candidateManifest = manifest,
                    lastCompletedStep = "compose_grids"
                )
            }
        }

        updateProgress(job.id, "ask_ai", "Asking AI", 0.7f)
        val candidates = candidatePaths.mapIndexed { index, path ->
            manifest.getOrNull(index)?.let { item ->
                VideoFrameCandidate(
                    filePath = path,
                    timestampMs = item.timestampMs,
                    sharpnessScore = item.sharpnessScore,
                    brightnessScore = item.brightnessScore,
                    differenceScore = item.differenceScore
                )
            } ?: VideoFrameCandidate(
                filePath = path,
                timestampMs = 0L,
                sharpnessScore = 0.0,
                brightnessScore = 0.0,
                differenceScore = 0.0
            )
        }
        val generated = apiRepository.generateVideoStickerPack(
            candidateGridPaths = gridPaths,
            candidateManifest = manifest,
            candidates = candidates,
            selectedStartMs = payload.selectedStartMs,
            selectedEndMs = payload.selectedEndMs,
            sourceDurationMs = payload.sourceDurationMs,
            prompt = payload.prompt
        )
        val stored = generated.toStored()
        val planJson = AiJobJson.codec.encodeToString(stored)
        val result = VideoPackResult(videoPlanResultJson = planJson)
        completeJob(job, AiJobJson.codec.encodeToString(result))
        mergeDraftContext(job.workspaceDraftId) { context ->
            context.copy(
                videoPlanResultJson = planJson,
                candidatePaths = candidatePaths,
                candidateGridPaths = gridPaths,
                candidateManifest = manifest,
                selectedStaticKeys = generated.staticStickers.map { it.selectionKey() }.toSet(),
                selectedAnimatedKeys = generated.animatedStickers.mapIndexed { index, sticker ->
                    sticker.selectionKey(index)
                }.toSet(),
                lastCompletedStep = "video_pack"
            )
        }
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.READY_TO_REVIEW)
    }

    private suspend fun runAnimatedEncode(job: AiJob) {
        val payload = AiJobJson.codec.decodeFromString<AnimatedEncodePayload>(job.payloadJson)
        val draft = draftRepository.getById(job.workspaceDraftId)
            ?: throw IllegalStateException("Workspace draft not found")
        val context = AiJobJson.codec.decodeFromString<WorkspaceDraftContext>(draft.contextJson)
        val frames = animatedDraftHelper.loadFrames(draft)
            ?: throw IllegalStateException("Animated frame files are missing")
        context.animatedFrameSnapshots.forEach { snapshot ->
            if (!SourcePathValidator.exists(snapshot.filePath)) {
                throw IllegalStateException("Animated frame file not found")
            }
        }
        updateProgress(job.id, "encode", "Encoding animated WebP", 0.1f)
        val outputPath = fileStorage.saveAnimatedStickerImage(
            frames = frames,
            fileName = payload.outputFileName,
            baseDecorations = context.animatedBaseDecorations,
            frameDecorations = context.animatedFrameDecorations,
            onProgress = { current, total ->
                val safeTotal = total.coerceAtLeast(1)
                val fraction = (current.toFloat() / safeTotal).coerceIn(0.1f, 0.95f)
                val composeUnits = frames.size.coerceAtLeast(1)
                val label = if (current <= composeUnits) {
                    "Composing $current / $composeUnits frames"
                } else {
                    "Encoding animated WebP"
                }
                runBlocking {
                    updateProgress(job.id, "encode", label, fraction)
                }
            }
        )
        val result = AnimatedEncodeResult(outputPath = outputPath)
        completeJob(job, AiJobJson.codec.encodeToString(result))
        mergeDraftContext(job.workspaceDraftId) { ctx ->
            ctx.copy(animatedOutputPath = outputPath)
        }
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.READY_TO_REVIEW)
    }

    private suspend fun splitGridWithProgress(
        jobId: String,
        rawGridPath: String,
        layout: String?
    ) = apiRepository.splitGridOnDevice(rawGridPath, layout) { update ->
        reportGridSplitProgress(jobId, update)
    }

    private suspend fun reportGridSplitProgress(
        jobId: String,
        update: GridSplitOnDeviceProgressUpdate
    ) {
        val stepKey = when (update.phase) {
            GridSplitProgressPhase.SplittingCells -> "grid_split_cells"
            GridSplitProgressPhase.ExtractingText -> "grid_text_assets"
            GridSplitProgressPhase.RemovingBackground -> "remove_background"
        }
        val fraction = gridSplitFraction(update)
        val label = update.stepLabel()
        updateProgress(jobId, stepKey, label, fraction)
        AiJobProgressLog.i(
            AI_JOB_LOG_TAG,
            "jobId=$jobId ${update.logLine()} fraction=$fraction label=$label"
        )
    }

    private companion object {
        private const val AI_JOB_LOG_TAG = "AiJobRunner"
    }

    private suspend fun handleFailure(job: AiJob, throwable: Throwable) {
        if (throwable is OutOfMemoryError) {
            AiJobProgressLog.e(
                AI_JOB_LOG_TAG,
                "jobId=${job.id} failed with OOM during on-device processing",
                throwable
            )
        }
        if (throwable is CancellationException) {
            val now = Clock.System.now().toEpochMilliseconds()
            jobRepository.update(
                job.copy(
                    status = AiJobStatus.QUEUED,
                    failureKind = AiJobFailureKind.NONE,
                    failureMessage = null,
                    completedAt = null,
                    updatedAt = now
                )
            )
            return
        }
        val (kind, retryable) = if (throwable is OutOfMemoryError) {
            AiJobFailureKind.LOCAL_PROCESSOR to true
        } else {
            AiJobFailureClassifier.classify(throwable)
        }
        val message = when (throwable) {
            is OutOfMemoryError ->
                "Memori penuh saat menghapus background. Tutup app lain lalu coba lagi."
            else -> throwable.message ?: "Processing failed"
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val exhausted = job.attemptCount >= AiJobManager.MAX_AUTO_RETRY_ATTEMPTS
        val status = when {
            kind == AiJobFailureKind.CANCELLED -> AiJobStatus.CANCELLED
            retryable && !exhausted -> AiJobStatus.FAILED_RETRYABLE
            else -> AiJobStatus.FAILED_FINAL
        }
        jobRepository.updateStatus(
            id = job.id,
            status = status,
            failureKind = kind,
            failureMessage = message,
            completedAt = now
        )
        draftRepository.updateJobLinks(
            id = job.workspaceDraftId,
            lastFailedJobId = job.id
        )
        draftRepository.updateStatus(job.workspaceDraftId, WorkspaceDraftStatus.NEEDS_ATTENTION)
        if (status == AiJobStatus.FAILED_RETRYABLE) {
            val nextRetryAt = jobManager.computeNextRetryAt(job.attemptCount)
            jobRepository.update(
                job.copy(
                    status = AiJobStatus.QUEUED,
                    failureKind = kind,
                    failureMessage = message,
                    nextRetryAt = nextRetryAt,
                    updatedAt = now
                )
            )
        }
    }

    private suspend fun completeJob(job: AiJob, resultJson: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        jobRepository.updateStatus(
            id = job.id,
            status = AiJobStatus.COMPLETED,
            resultJson = resultJson,
            completedAt = now
        )
        draftRepository.updateJobLinks(
            id = job.workspaceDraftId,
            lastCompletedJobId = job.id
        )
    }

    private suspend fun saveCheckpoint(job: AiJob, checkpoint: VideoPackCheckpoint) {
        val json = AiJobJson.codec.encodeToString(checkpoint)
        jobRepository.updateStatus(
            id = job.id,
            status = AiJobStatus.CHECKPOINTED,
            checkpointJson = json
        )
        jobRepository.update(job.copy(checkpointJson = json, status = AiJobStatus.RUNNING))
    }

    private suspend fun updateProgress(jobId: String, stepKey: String, stepLabel: String, fraction: Float) {
        if (isCancelled(jobId)) throw kotlinx.coroutines.CancellationException("Job cancelled")
        jobRepository.updateProgress(
            jobId,
            AiJobProgress(stepKey = stepKey, stepLabel = stepLabel, fraction = fraction)
        )
        AiJobProgressLog.d(
            AI_JOB_LOG_TAG,
            "progress jobId=$jobId step=$stepKey fraction=$fraction label=$stepLabel"
        )
    }

    private suspend fun isCancelled(jobId: String): Boolean {
        val status = jobRepository.getById(jobId)?.status
        return status == AiJobStatus.CANCEL_REQUESTED || status == AiJobStatus.CANCELLED
    }

    private suspend fun mergeDraftContext(
        draftId: String,
        transform: (WorkspaceDraftContext) -> WorkspaceDraftContext
    ) {
        val draft = draftRepository.getById(draftId) ?: return
        val context = AiJobJson.codec.decodeFromString<WorkspaceDraftContext>(draft.contextJson)
        val updated = transform(context)
        draftRepository.upsert(
            draft.copy(
                contextJson = AiJobJson.codec.encodeToString(updated),
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

}
