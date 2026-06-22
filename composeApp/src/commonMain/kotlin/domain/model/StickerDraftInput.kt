package domain.model

data class StickerDraftInput(
    val identifier: String,
    val name: String,
    val publisher: String,
    val visibility: String,
    val trayImagePath: String,
    /**
     * When true, [data.repository.StickerPackDraftSaver] throws if the tray cannot be
     * compressed to WhatsApp limits instead of leaving it empty (used for manual saves).
     */
    val strictTrayCompression: Boolean = false,
    val stickers: List<StickerInput>
) {
    data class StickerInput(
        val imagePath: String,
        val decorations: List<StickerDecoration> = emptyList(),
        val isAnimated: Boolean = false,
        val sourceVideoFile: String? = null,
        val frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap()
    )
}
