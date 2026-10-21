package data.sync

import data.local.entity.StickerEntity

/** Result of the pull pass; aggregated into [domain.model.SyncReport] by `SyncManagerImpl`. */
internal data class PullSyncResult(
    val packsDownloaded: Int,
    val stickersDownloaded: Int,
    val itemsDeleted: Int,
    val downloadFailures: Int,
)

internal data class RemotePackImportResult(
    val stickersImported: Int,
    val downloadFailures: Int,
)

internal data class RemoteStickerImportResult(
    val stickers: List<StickerEntity>,
    val downloadFailures: Int,
)
