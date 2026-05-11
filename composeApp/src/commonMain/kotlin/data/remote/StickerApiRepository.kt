package data.remote

import data.remote.mapper.toOverlayTextDecoration
import data.remote.model.ApiImage
import data.remote.model.GridSplitStickerFile
import data.storage.StickerFileStorage
import domain.error.AppErrorCode
import kotlin.random.Random

class StickerApiRepository(
    private val api: SetikerApiService,
    private val fileStorage: StickerFileStorage
) {
    suspend fun removeBackground(imagePath: String): String {
        val image = api.removeBackground(imagePath)
        return downloadAndPersist(image)
    }

    suspend fun generate(
        prompt: String,
        grid: Boolean,
        layout: String?,
        normalize: Boolean?,
        inputImagePath: String? = null
    ): List<String> {
        val images = api.generate(
            prompt = prompt,
            grid = grid,
            gridLayout = layout,
            normalize = normalize,
            inputImagePath = inputImagePath
        )
        return downloadAndPersistAll(images, operationTag = "generate")
    }

    suspend fun splitGrid(imagePath: String): List<GridSplitStickerFile> {
        val images = api.splitGrid(
            imagePath = imagePath
        )
        return downloadGridSplitFiles(images, operationTag = "grid-split")
    }

    private suspend fun downloadAndPersist(image: ApiImage): String {
        val bytes = api.downloadImageBytes(image.url)
        val safeId = image.id.ifBlank { Random.nextInt(1000, 9999).toString() }
        return fileStorage.saveBytes(
            bytes = bytes,
            fileName = "api_$safeId.png"
        )
    }

    private suspend fun downloadAndPersistAll(
        images: List<ApiImage>,
        operationTag: String
    ): List<String> {
        val results = mutableListOf<String>()
        images.forEach { image ->
            runCatching { downloadAndPersist(image) }
                .onSuccess { results += it }
                .onFailure {
                    println(
                        "StickerApiRepository[$operationTag]: failed image id=${image.id}, url=${image.url}, reason=${it.message}"
                    )
                }
        }
        if (results.isEmpty() && images.isNotEmpty()) {
            // If every download failed, surface a controlled error to UI.
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
                    val decoration = image.textOutsideForeground.toOverlayTextDecoration()
                    val decorations = listOfNotNull(decoration)
                    results += GridSplitStickerFile(localPath = path, decorations = decorations)
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
}
