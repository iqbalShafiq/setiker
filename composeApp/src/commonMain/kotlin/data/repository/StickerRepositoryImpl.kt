package data.repository

import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.storage.StickerFileStorage
import domain.error.AppErrorCode
import domain.error.AppException
import domain.model.Sticker
import domain.model.StickerDecoration
import domain.model.StickerPack
import domain.repository.StickerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StickerRepositoryImpl(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val fileStorage: StickerFileStorage
) : StickerRepository {

    override suspend fun getAllPacks(): List<StickerPack> = withContext(Dispatchers.IO) {
        packDao.getAll().map { it.toDomainModel(emptyList()) }
            .map { pack ->
                val stickers = stickerDao.getByPackId(pack.identifier)
                    .map { it.toDomainModel() }
                pack.copy(stickers = stickers)
            }
    }

    override suspend fun getPack(identifier: String): StickerPack = withContext(Dispatchers.IO) {
        val entity = packDao.getById(identifier)
            ?: throw AppException(code = AppErrorCode.PackNotFound)
        val stickers = stickerDao.getByPackId(identifier)
            .map { it.toDomainModel() }
        entity.toDomainModel(stickers)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun savePack(pack: StickerPack) = withContext(Dispatchers.IO) {
        val entity = StickerPackEntity(
            identifier = pack.identifier.takeIf { it.isNotBlank() } ?: Uuid.random().toString(),
            name = pack.name,
            publisher = pack.publisher,
            trayImageFile = pack.trayImageFile,
            updatedAt = System.currentTimeMillis()
        )
        packDao.insert(entity)

        // Save stickers
        pack.stickers.forEachIndexed { index, sticker ->
            val stickerEntity = StickerEntity(
                id = Uuid.random().toString(),
                packId = entity.identifier,
                imageFile = sticker.imageFile,
                sourceImageFile = sticker.sourceImageFile,
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                decorationsJson = Json.encodeToString(sticker.decorations),
                sortOrder = index
            )
            stickerDao.insert(stickerEntity)
        }
    }

    override suspend fun deletePack(identifier: String) = withContext(Dispatchers.IO) {
        val pack = packDao.getById(identifier) ?: return@withContext

        // Delete associated sticker files
        val stickers = stickerDao.getByPackId(identifier)
        stickers.forEach { sticker ->
            listOfNotNull(sticker.imageFile, sticker.sourceImageFile).distinct().forEach {
                fileStorage.deleteImage(it)
            }
        }
        fileStorage.deleteImage(pack.trayImageFile)

        // Delete from database
        stickerDao.deleteByPackId(identifier)
        packDao.delete(pack)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addStickerToPack(packId: String, sticker: Sticker) {
        withContext(Dispatchers.IO) {
            val count = stickerDao.getCountByPackId(packId)
            val entity = StickerEntity(
                id = Uuid.random().toString(),
                packId = packId,
                imageFile = sticker.imageFile,
                sourceImageFile = sticker.sourceImageFile,
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                decorationsJson = Json.encodeToString(sticker.decorations),
                sortOrder = count
            )
            stickerDao.insert(entity)

            // Update pack timestamp
            packDao.getById(packId)?.let { pack ->
                packDao.update(pack.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun updateStickerInPack(packId: String, index: Int, sticker: Sticker) {
        withContext(Dispatchers.IO) {
            val stickers = stickerDao.getByPackId(packId)
            val existing = stickers.getOrNull(index)
                ?: throw AppException(code = AppErrorCode.StickerNotFound)

            val updatedEntity = existing.copy(
                imageFile = sticker.imageFile,
                sourceImageFile = sticker.sourceImageFile,
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                decorationsJson = Json.encodeToString(sticker.decorations)
            )
            stickerDao.insert(updatedEntity)

            // Update pack timestamp
            packDao.getById(packId)?.let { pack ->
                packDao.update(pack.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun removeStickerFromPack(packId: String, index: Int) = withContext(Dispatchers.IO) {
        val stickers = stickerDao.getByPackId(packId)
        val stickerToDelete = stickers.getOrNull(index) ?: return@withContext

        listOfNotNull(stickerToDelete.imageFile, stickerToDelete.sourceImageFile).distinct().forEach {
            fileStorage.deleteImage(it)
        }
        stickerDao.deleteByPackAndIndex(packId, index)
    }

    private fun StickerPackEntity.toDomainModel(stickers: List<Sticker>) = StickerPack(
        identifier = identifier,
        name = name,
        publisher = publisher,
        trayImageFile = trayImageFile,
        stickers = stickers
    )

    private fun StickerEntity.toDomainModel() = Sticker(
        imageFile = imageFile,
        sourceImageFile = sourceImageFile,
        emojis = Json.decodeFromString(emojis),
        accessibilityText = accessibilityText,
        decorations = parseDecorations(decorationsJson)
    )

    private fun parseDecorations(raw: String?): List<StickerDecoration> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString<List<StickerDecoration>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}