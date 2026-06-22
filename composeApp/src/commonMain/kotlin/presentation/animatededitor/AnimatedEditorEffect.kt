package presentation.animatededitor

import domain.model.StickerDecoration
import presentation.common.UiText

sealed interface AnimatedEditorEffect {
    data object NavigateBack : AnimatedEditorEffect
    /**
     * Emitted after the animated WebP has been encoded to disk. The CreatePack
     * screen consumes [imagePath] (and the metadata) as a new draft sticker.
     */
    data class AnimatedDraftReady(
        val imagePath: String,
        val sourceVideoFile: String?,
        val baseDecorations: List<StickerDecoration>,
        val frameDecorations: Map<Int, List<StickerDecoration>>
    ) : AnimatedEditorEffect
    data class ShowError(val message: UiText) : AnimatedEditorEffect
}

