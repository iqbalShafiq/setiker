package data.remote.mapper

import data.remote.model.ApiTextOutsideForegroundStyle
import data.remote.model.ApiVideoAnimatedStickerPlan
import data.remote.model.ApiVideoAnimatedTimelineFrame
import data.remote.model.ApiVideoEmojiDecoration
import data.remote.model.ApiVideoRejectedCandidatePlan
import data.remote.model.ApiVideoStaticStickerPlan
import data.remote.model.ApiVideoStickerDecoration
import data.remote.model.ApiVideoStickerPackPlan
import data.remote.model.ApiVideoTextDecoration
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.EmojiDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.TextDecorationLayout
import domain.model.TextDecorationSource
import domain.model.VideoAnimatedStickerPlan
import domain.model.VideoAnimatedTimelineFrame
import domain.model.VideoRejectedCandidatePlan
import domain.model.VideoStaticStickerPlan
import domain.model.VideoStickerPackPlan

fun ApiVideoStickerPackPlan.toDomain(): VideoStickerPackPlan = VideoStickerPackPlan(
    packTitle = packTitle,
    summary = summary,
    staticStickers = staticStickers.map { it.toDomain() },
    animatedStickers = animatedStickers.map { it.toDomain() },
    rejectedCandidates = rejectedCandidates.map { it.toDomain() }
)

private fun ApiVideoStaticStickerPlan.toDomain(): VideoStaticStickerPlan = VideoStaticStickerPlan(
    candidateId = candidateId,
    frameIndex = frameIndex,
    timestampMs = timestampMs,
    cellId = cellId,
    emojis = emojis,
    accessibilityText = accessibilityText,
    decorations = decorations.mapIndexed { index, decoration -> decoration.toDomain("static_${candidateId}_$index") },
    rationale = rationale
)

private fun ApiVideoAnimatedStickerPlan.toDomain(): VideoAnimatedStickerPlan = VideoAnimatedStickerPlan(
    timeline = timeline.map { it.toDomain() },
    fps = fps,
    loopCount = loopCount,
    emojis = emojis,
    accessibilityText = accessibilityText,
    baseDecorations = baseDecorations.mapIndexed { index, decoration -> decoration.toDomain("anim_base_$index") },
    frameDecorations = frameDecorations.associate { entry ->
        entry.frameIndex to entry.decorations.mapIndexed { index, decoration ->
            decoration.toDomain("anim_frame_${entry.frameIndex}_$index")
        }
    },
    rationale = rationale
)

private fun ApiVideoAnimatedTimelineFrame.toDomain(): VideoAnimatedTimelineFrame = VideoAnimatedTimelineFrame(
    candidateId = candidateId,
    frameIndex = frameIndex,
    timestampMs = timestampMs,
    durationMs = durationMs
)

private fun ApiVideoRejectedCandidatePlan.toDomain(): VideoRejectedCandidatePlan = VideoRejectedCandidatePlan(
    candidateId = candidateId,
    reason = reason
)

private fun ApiVideoStickerDecoration.toDomain(id: String): StickerDecoration = when (this) {
    is ApiVideoTextDecoration -> TextDecoration(
        id = "api_video_text_$id",
        text = text,
        font = style.toDecorationFont(),
        fontWeight = style.toDecorationFontWeight(),
        textColorArgb = parseApiColorToArgb(style?.color) ?: 0xFFFFFFFFL,
        style = domain.model.TextDecorationStyle.ApiCaption,
        source = TextDecorationSource.ApiOutsideForeground,
        layout = TextDecorationLayout.BottomCaption,
        centerX = centerX,
        centerY = centerY,
        scale = scale
    )
    is ApiVideoEmojiDecoration -> EmojiDecoration(
        id = "api_video_emoji_$id",
        emoji = emoji,
        centerX = centerX,
        centerY = centerY,
        scale = scale
    )
}

private fun ApiTextOutsideForegroundStyle?.toDecorationFont(): DecorationFont {
    val value = this?.fontFamily?.lowercase().orEmpty()
    return when {
        "mono" in value -> DecorationFont.Mono
        "serif" in value && "sans" !in value -> DecorationFont.Serif
        "rounded" in value -> DecorationFont.Rounded
        "condensed" in value -> DecorationFont.Condensed
        "display" in value -> DecorationFont.Display
        "cursive" in value || "script" in value -> DecorationFont.Cursive
        else -> DecorationFont.Sans
    }
}

private fun ApiTextOutsideForegroundStyle?.toDecorationFontWeight(): DecorationFontWeight {
    val value = this?.weight?.lowercase().orEmpty()
    val numeric = value.filter { it.isDigit() }.toIntOrNull()
    return when {
        numeric != null && numeric >= 700 -> DecorationFontWeight.Bold
        numeric != null && numeric >= 600 -> DecorationFontWeight.SemiBold
        numeric != null && numeric >= 500 -> DecorationFontWeight.Medium
        numeric != null && numeric <= 300 -> DecorationFontWeight.Light
        "bold" in value -> DecorationFontWeight.Bold
        "semi" in value || "demi" in value -> DecorationFontWeight.SemiBold
        "medium" in value -> DecorationFontWeight.Medium
        "light" in value || "thin" in value -> DecorationFontWeight.Light
        else -> DecorationFontWeight.Regular
    }
}
