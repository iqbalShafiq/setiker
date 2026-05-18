package data.remote

import data.remote.mapper.toDecorationApiImage
import data.remote.mapper.toStickerDecorations
import data.remote.model.ApiImage
import data.remote.model.GeneratedStickerFile
import data.remote.model.GridSplitStickerFile
import data.storage.StickerFileStorage
import data.util.OnDeviceImageProcessor
import data.util.parseGridLayout
import domain.error.AppErrorCode
import kotlinx.datetime.Clock
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
        inputImagePath: String? = null
    ): List<GeneratedStickerFile> {
        val images = api.generate(
            prompt = prompt,
            inputImagePath = inputImagePath
        )
        return downloadGeneratedStickerFiles(images, operationTag = "generate")
    }

    suspend fun generateStickerPack(
        prompt: String,
        layout: String,
        inputImagePath: String? = null
    ): List<GridSplitStickerFile> {
        val images = api.generateStickerPack(
            prompt = prompt,
            layout = layout,
            inputImagePath = inputImagePath
        )
        val rawGridPath = images.firstOrNull()?.let { downloadAndPersist(it) }
            ?: return emptyList()
        return splitGridOnDevice(rawGridPath, layout)
    }

    suspend fun generateVideoStickerPack(
        candidateGridPaths: List<String>,
        candidateCount: Int,
        selectedStartMs: Long,
        selectedEndMs: Long,
        sourceDurationMs: Long,
        prompt: String? = null
    ): List<GridSplitStickerFile> {
        val images = api.generateVideoStickerPack(
            candidateGridPaths = candidateGridPaths,
            candidateCount = candidateCount,
            selectedStartMs = selectedStartMs,
            selectedEndMs = selectedEndMs,
            sourceDurationMs = sourceDurationMs,
            prompt = prompt
        )
        val rawGridPath = images.firstOrNull()?.let { downloadAndPersist(it) }
            ?: return emptyList()
        return splitGridOnDevice(rawGridPath, "4x4")
    }

    suspend fun improve(imagePaths: List<String>): List<GeneratedStickerFile> {
        val images = api.improve(imagePaths)
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
        layout: String?
    ): List<GridSplitStickerFile> {
        val (rows, cols) = parseGridLayout(layout)
        val rawCellPaths = onDeviceImageProcessor.splitGridRawCells(
            imagePath = imagePath,
            rows = rows,
            cols = cols
        )
        val textAssets = extractTextAssetsOrEmpty(rawCellPaths)
        return rawCellPaths.mapIndexed { index, rawCellPath ->
            val processedPath = onDeviceImageProcessor.removeBackground(rawCellPath)
            GridSplitStickerFile(
                localPath = processedPath,
                rawCellPath = rawCellPath,
                decorations = textAssets.getOrNull(index)
                    ?.toDecorationApiImage()
                    ?.toStickerDecorations()
                    .orEmpty()
            )
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
            .onFailure {
                println(
                    "StickerApiRepository[grid-text-assets]: failed to extract text assets reason=${it.message}"
                )
            }
            .getOrElse { emptyList() }
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
