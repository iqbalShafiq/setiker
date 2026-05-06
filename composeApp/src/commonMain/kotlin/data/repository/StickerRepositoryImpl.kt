package data.repository

import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.storage.StickerFileStorage
import domain.model.Sticker
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
            ?: throw IllegalArgumentException("Pack not found: $identifier")
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
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
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
            fileStorage.deleteImage(sticker.imageFile)
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
                emojis = Json.encodeToString(sticker.emojis),
                accessibilityText = sticker.accessibilityText,
                sortOrder = count
            )
            stickerDao.insert(entity)

            // Update pack timestamp
            packDao.getById(packId)?.let { pack ->
                packDao.insert(pack.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun removeStickerFromPack(packId: String, index: Int) = withContext(Dispatchers.IO) {
        val stickers = stickerDao.getByPackId(packId)
        val stickerToDelete = stickers.getOrNull(index) ?: return@withContext

        fileStorage.deleteImage(stickerToDelete.imageFile)
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
        emojis = Json.decodeFromString(emojis),
        accessibilityText = accessibilityText
    )
}