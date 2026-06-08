package data.local.database

import data.local.entity.StickerEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class InMemoryStickerDao : StickerDao {
    private val stickers = linkedMapOf<String, StickerEntity>()

    override suspend fun getByPackId(packId: String): List<StickerEntity> =
        stickers.values.filter { it.packId == packId }.sortedBy { it.sortOrder }

    override suspend fun getByCloudId(cloudId: String): StickerEntity? =
        stickers.values.firstOrNull { it.cloudId == cloudId }

    override suspend fun insert(sticker: StickerEntity) {
        stickers[sticker.id] = sticker
    }

    override suspend fun deleteByPackAndIndex(packId: String, index: Int) {
        stickers.values
            .filter { it.packId == packId && it.sortOrder == index }
            .forEach { stickers.remove(it.id) }
    }

    override suspend fun deleteByPackId(packId: String) {
        stickers.entries.removeIf { it.value.packId == packId }
    }

    override suspend fun deleteByCloudId(cloudId: String) {
        stickers.entries.removeIf { it.value.cloudId == cloudId }
    }

    override suspend fun deleteById(id: String) {
        stickers.remove(id)
    }

    override suspend fun getCountByPackId(packId: String): Int =
        stickers.values.count { it.packId == packId }

    override suspend fun getBySyncState(state: String): List<StickerEntity> =
        stickers.values.filter { it.syncState == state }

    override suspend fun getUnsynced(): List<StickerEntity> =
        stickers.values.filter { it.syncState != "SYNCED" }

    override suspend fun updateSyncStatus(id: String, syncState: String, lastSyncAt: Long?) {
        stickers[id]?.let { stickers[id] = it.copy(syncState = syncState, lastSyncAt = lastSyncAt) }
    }

    override suspend fun updateCloudInfo(id: String, cloudId: String, cloudUrl: String?) {
        stickers[id]?.let { stickers[id] = it.copy(cloudId = cloudId, cloudUrl = cloudUrl) }
    }
}

class StickerSortOrderTest {

    @Test
    fun compactSortOrdersRenormalizesGapsAfterDeletion() = runTest {
        val dao = InMemoryStickerDao()
        val packId = "pack-1"
        dao.insert(
            StickerEntity(
                id = "a",
                packId = packId,
                imageFile = "a.webp",
                emojis = "[]",
                accessibilityText = null,
                sortOrder = 0,
            )
        )
        dao.insert(
            StickerEntity(
                id = "c",
                packId = packId,
                imageFile = "c.webp",
                emojis = "[]",
                accessibilityText = null,
                sortOrder = 2,
            )
        )

        dao.compactSortOrders(packId)

        assertEquals(listOf(0, 1), dao.getByPackId(packId).map { it.sortOrder })
    }
}
