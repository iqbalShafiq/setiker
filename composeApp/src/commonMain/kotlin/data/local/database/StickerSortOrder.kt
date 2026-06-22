package data.local.database

suspend fun StickerDao.compactSortOrders(packId: String) {
    getByPackId(packId).forEachIndexed { index, sticker ->
        if (sticker.sortOrder != index) {
            insert(sticker.copy(sortOrder = index))
        }
    }
}
