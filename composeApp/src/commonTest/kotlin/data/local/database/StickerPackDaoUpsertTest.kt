package data.local.database

import data.local.entity.StickerEntity
import data.local.entity.StickerPackEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Documents why metadata-only pack writes must use [StickerPackDao.upsert] instead of
 * [StickerPackDao.insert] on an existing row (REPLACE + FK CASCADE deletes stickers).
 */
class StickerPackDaoUpsertTest {

    @Test
    fun upsertUpdatesExistingPackWithoutClearingStickers() = runTest {
        val dao = InMemoryStickerPackDao()
        val pack = samplePack(visibility = "PRIVATE")
        dao.insert(pack)
        dao.stickers += stickerRow(packId = pack.identifier, id = "s1")

        dao.upsert(pack.copy(visibility = "PUBLIC", updatedAt = 99L))

        assertEquals("PUBLIC", dao.getById(pack.identifier)?.visibility)
        assertEquals(1, dao.stickers.count { it.packId == pack.identifier })
    }

    @Test
    fun replaceInsertOnExistingPackClearsStickersInCascadeModel() = runTest {
        val dao = InMemoryStickerPackDao(simulateReplaceCascade = true)
        val pack = samplePack(visibility = "PRIVATE")
        dao.insert(pack)
        dao.stickers += stickerRow(packId = pack.identifier, id = "s1")

        dao.insert(pack.copy(visibility = "PUBLIC", updatedAt = 99L))

        assertEquals("PUBLIC", dao.getById(pack.identifier)?.visibility)
        assertEquals(0, dao.stickers.count { it.packId == pack.identifier })
    }

    private fun samplePack(visibility: String) = StickerPackEntity(
        identifier = "pack-1",
        name = "Pack",
        publisher = "Setiker",
        trayImageFile = "tray.webp",
        visibility = visibility,
    )

    private fun stickerRow(packId: String, id: String) = StickerEntity(
        id = id,
        packId = packId,
        imageFile = "$id.webp",
        emojis = "[]",
        accessibilityText = null,
        sortOrder = 0,
    )

    private class InMemoryStickerPackDao(
        private val simulateReplaceCascade: Boolean = false,
    ) : StickerPackDao {
        val stickers = mutableListOf<StickerEntity>()
        private val packs = mutableMapOf<String, StickerPackEntity>()

        override suspend fun getAll(): List<StickerPackEntity> = packs.values.toList()

        override suspend fun getById(id: String): StickerPackEntity? = packs[id]

        override suspend fun insert(pack: StickerPackEntity) {
            if (simulateReplaceCascade && packs.containsKey(pack.identifier)) {
                stickers.removeAll { it.packId == pack.identifier }
            }
            packs[pack.identifier] = pack
        }

        override suspend fun update(pack: StickerPackEntity) {
            packs[pack.identifier] = pack
        }

        override suspend fun upsert(pack: StickerPackEntity) {
            if (getById(pack.identifier) != null) update(pack) else insert(pack)
        }

        override suspend fun delete(pack: StickerPackEntity) {
            packs.remove(pack.identifier)
            stickers.removeAll { it.packId == pack.identifier }
        }

        override suspend fun getBySyncState(state: String): List<StickerPackEntity> =
            packs.values.filter { it.syncState == state }

        override suspend fun getUnsynced(): List<StickerPackEntity> =
            packs.values.filter { it.syncState != "SYNCED" && it.syncState != "SUCCESS" }

        override suspend fun getByCloudId(cloudId: String): StickerPackEntity? =
            packs.values.firstOrNull { it.cloudId == cloudId }

        override suspend fun updateSyncStatus(identifier: String, syncState: String, lastSyncAt: Long?) {
            packs[identifier]?.let { packs[identifier] = it.copy(syncState = syncState, lastSyncAt = lastSyncAt) }
        }

        override suspend fun updateCloudId(identifier: String, cloudId: String) {
            packs[identifier]?.let { packs[identifier] = it.copy(cloudId = cloudId) }
        }
    }
}
