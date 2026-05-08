package data.remote

import data.remote.model.ApiImage
import data.storage.StickerFileStorage
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
        normalize: Boolean?
    ): List<String> {
        return api.generate(
            prompt = prompt,
            grid = grid,
            gridLayout = layout,
            normalize = normalize
        ).map { image ->
            downloadAndPersist(image)
        }
    }

    suspend fun splitGrid(imagePath: String): List<String> {
        return api.splitGrid(
            imagePath = imagePath
        ).map { image ->
            downloadAndPersist(image)
        }
    }

    private suspend fun downloadAndPersist(image: ApiImage): String {
        val bytes = api.downloadImageBytes(image.url)
        val safeId = image.id.ifBlank { Random.nextInt(1000, 9999).toString() }
        return fileStorage.saveBytes(
            bytes = bytes,
            fileName = "api_$safeId.png"
        )
    }
}
