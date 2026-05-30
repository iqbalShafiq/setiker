package data.remote

enum class GridSplitProgressPhase {
    SplittingCells,
    ExtractingText,
    RemovingBackground
}

data class GridSplitOnDeviceProgressUpdate(
    val phase: GridSplitProgressPhase,
    val current: Int,
    val total: Int
) {
    fun fractionInPhase(): Float =
        if (total <= 0) 0f else current.toFloat() / total.coerceAtLeast(1)

    /** Counter (X/Y) only for on-device phases; API steps use a static label. */
    fun stepLabel(): String = when (phase) {
        GridSplitProgressPhase.SplittingCells ->
            if (showsCounter) "Memotong grid $current dari $total" else "Memotong grid…"
        GridSplitProgressPhase.ExtractingText -> "Mengekstrak teks…"
        GridSplitProgressPhase.RemovingBackground ->
            "Menghapus background stiker $current dari $total"
    }

    fun logLine(): String = when (phase) {
        GridSplitProgressPhase.SplittingCells ->
            if (showsCounter) "grid_split_cells $current/$total" else "grid_split_cells"
        GridSplitProgressPhase.ExtractingText -> "grid_text_assets_api"
        GridSplitProgressPhase.RemovingBackground -> "remove_background $current/$total"
    }

    private val showsCounter: Boolean
        get() = phase != GridSplitProgressPhase.ExtractingText && total > 0 && current > 0
}

typealias GridSplitOnDeviceProgressListener = suspend (GridSplitOnDeviceProgressUpdate) -> Unit

internal fun gridSplitFraction(
    update: GridSplitOnDeviceProgressUpdate,
    removingBackgroundStart: Float = 0.55f,
    removingBackgroundEnd: Float = 0.82f
): Float = when (update.phase) {
    GridSplitProgressPhase.SplittingCells -> 0.48f + 0.04f * update.fractionInPhase()
    GridSplitProgressPhase.ExtractingText -> 0.54f
    GridSplitProgressPhase.RemovingBackground ->
        removingBackgroundStart +
            (removingBackgroundEnd - removingBackgroundStart) * update.fractionInPhase()
}
