package data.repository

import data.local.database.StickerDao
import data.local.database.StickerPackDao
import data.local.database.SyncOperationDao
import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import data.storage.StickerFileStorage
import data.sync.SyncCursorStore

class CloudSyncedLocalDataCleaner(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao,
    private val syncOperationDao: SyncOperationDao,
    private val syncCursorStore: SyncCursorStore,
    private val fileStorage: StickerFileStorage,
) {
    suspend fun clearOnLogout() {
        val cloudSyncedPacks = packDao.getAll().filter { !it.cloudId.isNullOrBlank() }
        cloudSyncedPacks.forEach { pack ->
            deletePackFiles(pack)
            stickerDao.deleteByPackId(pack.identifier)
            packDao.delete(pack)
        }

        packDao.getAll().forEach { pack ->
            stickerDao.getByPackId(pack.identifier)
                .filter { !it.cloudId.isNullOrBlank() }
                .forEach { sticker ->
                    deleteStickerFiles(sticker)
                    stickerDao.deleteById(sticker.id)
                }
        }

        syncOperationDao.clearAll()
        syncCursorStore.clear()
    }

    private suspend fun deletePackFiles(pack: StickerPackEntity) {
        for (sticker in stickerDao.getByPackId(pack.identifier)) {
            deleteStickerFiles(sticker)
        }
        if (pack.trayImageFile.isNotBlank()) {
            fileStorage.deleteImage(pack.trayImageFile)
        }
    }

    private suspend fun deleteStickerFiles(sticker: StickerEntity) {
        listOfNotNull(sticker.imageFile, sticker.sourceImageFile)
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { fileStorage.deleteImage(it) }
    }
}
