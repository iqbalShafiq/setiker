package data.remote

import data.aijob.AiJobProgressLog
import data.remote.mapper.toDecorationApiImage
import data.util.deleteLocalFileQuietly
import data.util.releaseMemoryAfterStickerStep
import data.remote.mapper.toDomain
import data.remote.mapper.toStickerDecorations
import data.remote.model.ApiImage
import data.remote.model.GeneratedStickerFile
import data.remote.model.GridSplitStickerFile
import data.storage.StickerFileStorage
import data.util.OnDeviceImageProcessor
import data.util.parseGridLayout
import domain.error.AppErrorCode
import domain.model.ResolvedVideoAnimatedSticker
import domain.model.ResolvedVideoAnimatedTimelineFrame
import domain.model.ResolvedVideoStaticSticker
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.VideoFrameCandidate
import domain.model.VideoStickerCandidateManifestItem
import kotlin.time.Clock
import kotlin.random.Random

class StickerApiRepository(
    private val api: SetikerApiService,
    private val fileStorage: StickerFileStorage,
    private val onDeviceImageProcessor: OnDeviceImageProcessor
) {
    suspend fun removeBackground(imagePath: String): String {
        val image = api.removeBackground(imagePath)
        return downloadAndPersist(image)
    }

    suspend fun generateStickers(
        prompt: String,
        inputImagePath: String? = null,
        reservationId: String? = null
    ): List<GeneratedStickerFile> {
        val images = api.generate(
            prompt = prompt,
            inputImagePath = inputImagePath,
            reservationId = reservationId
        )
        return downloadGeneratedStickerFiles(images, operationTag = "generate")
    }

    suspend fun generateStickerPack(
        prompt: String,
        layout: String,
        inputImagePath: String? = null
    ): List<GridSplitStickerFile> {
        val rawGridPath = fetchGeneratePackGridPath(
            prompt = prompt,
            layout = layout,
            inputImagePath = inputImagePath
        ) ?: return emptyList()
        return splitGridOnDevice(rawGridPath, layout)
    }

    suspend fun fetchGeneratePackGridPath(
        prompt: String,
        layout: String,
        inputImagePath: String? = null,
        reservationId: String? = null
    ): String? {
        val images = api.generateStickerPack(
            prompt = prompt,
            layout = layout,
            inputImagePath = inputImagePath,
            reservationId = reservationId
        )
        return images.firstOrNull()?.let { downloadAndPersist(it) }
    }

    suspend fun generateVideoStickerPack(
        candidateGridPaths: List<String>,
        candidateManifest: List<VideoStickerCandidateManifestItem>,
        candidates: List<VideoFrameCandidate>,
        selectedStartMs: Long,
        selectedEndMs: Long,
        sourceDurationMs: Long,
        prompt: String? = null,
        reservationId: String? = null
    ): ResolvedVideoStickerPackPlan {
        val plan = api.generateVideoStickerPack(
            candidateGridPaths = candidateGridPaths,
            candidateManifest = candidateManifest,
            selectedStartMs = selectedStartMs,
            selectedEndMs = selectedEndMs,
            sourceDurationMs = sourceDurationMs,
            prompt = prompt,
            reservationId = reservationId
        ).toDomain()
        val candidatesById = candidateManifest.associate { manifestItem ->
            val candidate = candidates.getOrNull(manifestItem.frameIndex)
                ?: throw ApiException(code = AppErrorCode.InvalidGenerateResponse)
            manifestItem.candidateId to candidate
        }

        val resolvedStatic = plan.staticStickers.map { sticker ->
            val candidate = candidatesById[sticker.candidateId]
                ?: throw ApiException(code = AppErrorCode.InvalidGenerateResponse)
            ResolvedVideoStaticSticker(plan = sticker, localPath = candidate.filePath)
        }

        val resolvedAnimated = plan.animatedStickers.map { sticker ->
            ResolvedVideoAnimatedSticker(
                plan = sticker,
                timeline = sticker.timeline.map { frame ->
                    val candidate = if (frame.candidateId != null) {
                        candidatesById[frame.candidateId]
                    } else {
                        candidates.getOrNull(frame.frameIndex)
                    } ?: throw ApiException(code = AppErrorCode.InvalidGenerateResponse)
                    ResolvedVideoAnimatedTimelineFrame(frame = frame, localPath = candidate.filePath)
                }
            )
        }

        return ResolvedVideoStickerPackPlan(
            plan = plan,
            staticStickers = resolvedStatic,
            animatedStickers = resolvedAnimated
        )
    }

    suspend fun improve(
        imagePaths: List<String>,
        reservationId: String? = null
    ): List<GeneratedStickerFile> {
        val images = api.improve(imagePaths, reservationId = reservationId)
        if (imagePaths.size <= 1) {
            return downloadGeneratedStickerFiles(images, operationTag = "improve")
        }

        val chunkSizes = imagePaths.chunked(16).map { it.size }
        validateImproveGridResponseCount(images = images, chunkSizes = chunkSizes)
        return images.zip(chunkSizes).flatMap { (image, cellCount) ->
            val rawGridPath = downloadAndPersist(image)
            splitGridOnDevice(rawGridPath, "4x4")
                .take(cellCount)
                .map { splitFile ->
                    GeneratedStickerFile(
                        localPath = splitFile.localPath,
                        decorations = splitFile.decorations
                    )
                }
        }
    }

    suspend fun splitGridOnDevice(
        imagePath: String,
        layout: String?,
        onProgress: GridSplitOnDeviceProgressListener = {}
    ): List<GridSplitStickerFile> {
        val (rows, cols) = parseGridLayout(layout)
        val cellCount = rows * cols
        AiJobProgressLog.i(LOG_TAG, "splitGridOnDevice start layout=${rows}x$cols cells=$cellCount")
        onProgress(
            GridSplitOnDeviceProgressUpdate(
                phase = GridSplitProgressPhase.SplittingCells,
                current = 0,
                total = 0
            )
        )
        val rawCellPaths = onDeviceImageProcessor.splitGridRawCells(
            imagePath = imagePath,
            rows = rows,
            cols = cols
        )
        onProgress(
            GridSplitOnDeviceProgressUpdate(
                phase = GridSplitProgressPhase.SplittingCells,
                current = rawCellPaths.size,
                total = rawCellPaths.size
            )
        )
        AiJobProgressLog.i(
            LOG_TAG,
            "splitGridOnDevice cells_ready count=${rawCellPaths.size}"
        )

        val total = rawCellPaths.size
        onProgress(
            GridSplitOnDeviceProgressUpdate(
                phase = GridSplitProgressPhase.ExtractingText,
                current = 0,
                total = 0
            )
        )
        AiJobProgressLog.i(LOG_TAG, "splitGridOnDevice text_assets_request cells=$total")
        val textAssets = extractTextAssetsOrEmpty(rawCellPaths)
        AiJobProgressLog.i(
            LOG_TAG,
            "splitGridOnDevice text_assets_done decorations=${textAssets.size}"
        )

        return rawCellPaths.mapIndexed { index, rawCellPath ->
            val stickerIndex = index + 1
            onProgress(
                GridSplitOnDeviceProgressUpdate(
                    phase = GridSplitProgressPhase.RemovingBackground,
                    current = stickerIndex,
                    total = total
                )
            )
            AiJobProgressLog.i(
                LOG_TAG,
                "remove_background start sticker=$stickerIndex/$total"
            )
            val processedPath = onDeviceImageProcessor.removeBackground(rawCellPath)
            AiJobProgressLog.i(
                LOG_TAG,
                "remove_background done sticker=$stickerIndex/$total output=${processedPath.takeLast(64)}"
            )
            deleteLocalFileQuietly(rawCellPath)
            releaseMemoryAfterStickerStep()
            GridSplitStickerFile(
                localPath = processedPath,
                rawCellPath = rawCellPath,
                decorations = textAssets.getOrNull(index)
                    ?.toDecorationApiImage()
                    ?.toStickerDecorations()
                    .orEmpty()
            )
        }.also {
            AiJobProgressLog.i(LOG_TAG, "splitGridOnDevice complete stickers=${it.size}")
        }
    }

    suspend fun splitGrid(imagePath: String): List<GridSplitStickerFile> {
        val images = api.splitGrid(
            imagePath = imagePath
        )
        return downloadGridSplitFiles(images, operationTag = "grid-split")
    }

    private suspend fun downloadAndPersist(image: ApiImage): String {
        val bytes = api.downloadImageBytes(image.url)
        val safeId = image.id
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank {
                "${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100000, 999999)}"
            }
        return fileStorage.saveBytes(
            bytes = bytes,
            fileName = "api_$safeId.png"
        )
    }

    private suspend fun downloadAndPersistGenerated(image: ApiImage): GeneratedStickerFile =
        image.toGeneratedStickerFile(localPath = downloadAndPersist(image))

    private suspend fun downloadGeneratedStickerFiles(
        images: List<ApiImage>,
        operationTag: String
    ): List<GeneratedStickerFile> {
        val results = mutableListOf<GeneratedStickerFile>()
        images.forEach { image ->
            runCatching { downloadAndPersistGenerated(image) }
                .onSuccess { results += it }
                .onFailure {
                    println(
                        "StickerApiRepository[$operationTag]: failed image id=${image.id}, url=${image.url}, reason=${it.message}"
                    )
                }
        }
        if (results.isEmpty() && images.isNotEmpty()) {
            throw ApiException(code = AppErrorCode.ImageDownloadFailed)
        }
        return results
    }

    private suspend fun downloadGridSplitFiles(
        images: List<ApiImage>,
        operationTag: String
    ): List<GridSplitStickerFile> {
        val results = mutableListOf<GridSplitStickerFile>()
        images.forEach { image ->
            runCatching { downloadAndPersist(image) }
                .onSuccess { path ->
                    results += GridSplitStickerFile(
                        localPath = path,
                        decorations = image.toStickerDecorations()
                    )
                }
                .onFailure {
                    println(
                        "StickerApiRepository[$operationTag]: failed image id=${image.id}, url=${image.url}, reason=${it.message}"
                    )
                }
        }
        if (results.isEmpty() && images.isNotEmpty()) {
            throw ApiException(code = AppErrorCode.ImageDownloadFailed)
        }
        return results
    }

    private suspend fun extractTextAssetsOrEmpty(
        rawCellPaths: List<String>
    ) = runCatching { api.extractGridTextAssets(rawCellPaths) }
            .onFailure { error ->
                AiJobProgressLog.w(
                    LOG_TAG,
                    "grid_text_assets failed cells=${rawCellPaths.size} reason=${error.message}",
                    error
                )
            }
            .getOrElse { emptyList() }

    private companion object {
        private const val LOG_TAG = "StickerApiRepository"
    }
}

internal fun ApiImage.toGeneratedStickerFile(localPath: String): GeneratedStickerFile =
    GeneratedStickerFile(
        localPath = localPath,
        decorations = toStickerDecorations()
    )

internal fun validateImproveGridResponseCount(
    images: List<ApiImage>,
    chunkSizes: List<Int>
) {
    if (images.size != chunkSizes.size) {
        throw ApiException(code = AppErrorCode.InvalidGenerateResponse)
    }
}
