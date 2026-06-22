package data.util

import data.remote.model.GridSplitStickerFile

class IosOnDeviceImageProcessor : OnDeviceImageProcessor {
    override suspend fun removeBackground(imagePath: String): String {
        throw UnsupportedOperationException("On-device background removal is not implemented on iOS yet.")
    }

    override suspend fun splitGrid(
        imagePath: String,
        rows: Int,
        cols: Int
    ): List<GridSplitStickerFile> {
        throw UnsupportedOperationException("On-device grid split is not implemented on iOS yet.")
    }

    override suspend fun splitGridRawCells(
        imagePath: String,
        rows: Int,
        cols: Int
    ): List<String> {
        throw UnsupportedOperationException("On-device grid split is not implemented on iOS yet.")
    }
}
