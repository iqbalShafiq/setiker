package data.util

import data.remote.model.GridSplitStickerFile

interface OnDeviceImageProcessor {
    suspend fun removeBackground(imagePath: String): String

    suspend fun splitGrid(
        imagePath: String,
        rows: Int,
        cols: Int
    ): List<GridSplitStickerFile>

    suspend fun splitGridRawCells(
        imagePath: String,
        rows: Int,
        cols: Int
    ): List<String>
}

fun parseGridLayout(layout: String?): Pair<Int, Int> {
    val value = layout?.trim().orEmpty()
    val match = Regex("^(\\d+)\\s*[x×]\\s*(\\d+)$", RegexOption.IGNORE_CASE).matchEntire(value)
    val rows = match?.groupValues?.getOrNull(1)?.toIntOrNull()
    val cols = match?.groupValues?.getOrNull(2)?.toIntOrNull()
    return if (rows != null && cols != null && rows > 0 && cols > 0) {
        rows to cols
    } else {
        4 to 4
    }
}
