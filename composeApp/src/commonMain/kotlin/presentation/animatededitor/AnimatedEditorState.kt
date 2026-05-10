package presentation.animatededitor

import domain.model.DecodedFrame
import domain.model.Sticker
import domain.model.StickerDecoration

enum class DecorationApplyScope {
    AllFrames,
    CurrentFrameOnly
}

data class AnimatedEditorState(
    val draftId: String = "",
    val videoPath: String = "",
    val frames: List<DecodedFrame> = emptyList(),
    val currentFrameIndex: Int = 0,
    val baseDecorations: List<StickerDecoration> = emptyList(),
    val frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap(),
    val selectedDecorationId: String? = null,
    val applyScope: DecorationApplyScope = DecorationApplyScope.AllFrames,
    val isPlaying: Boolean = false,
    val emojis: List<String> = listOf("⭐"),
    val accessibilityText: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveProgress: Float = 0f,
    val saveProgressLabel: String? = null,
    val errorMessage: String? = null,
    val showEmojiPicker: Boolean = false,
    val showTextDecorationSheet: Boolean = false,
    val showDecorationEmojiPicker: Boolean = false,
    val recentEmojis: List<String> = emptyList(),
    val decorationEmojiPickerTargetId: String? = null
) {
    val totalFrames: Int get() = frames.size

    val visibleDecorations: List<StickerDecoration>
        get() = baseDecorations + (frameDecorations[currentFrameIndex] ?: emptyList())

    companion object {
        const val MAX_EMOJIS_PER_STICKER = Sticker.MAX_EMOJIS
    }
}
