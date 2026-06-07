package domain.model

object DecorationRenderSpec {
    const val MIN_SCALE = 0.3f
    const val MAX_SCALE = 4f

    const val TEXT_BOX_RATIO = 0.32f
    const val TEXT_SIZE_RATIO = 0.13f

    const val EMOJI_BOX_RATIO = 0.24f
    const val EMOJI_SIZE_RATIO = 0.18f

    const val IMAGE_BASE_RATIO = 0.35f

    /**
     * Grid-split API outside-foreground captions: larger default type than manual editor text
     * ([TEXT_SIZE_RATIO]); tuned for readability when exported to WhatsApp sticker size.
     */
    const val API_CAPTION_TEXT_SIZE_RATIO = 0.188f

    /** Horizontal inset from sticker edge as a fraction of canvas width (caption max width uses the inset twice). */
    const val API_CAPTION_HORIZONTAL_INSET_RATIO = 0.032f

    fun textBoxWidthPx(
        decoration: TextDecoration,
        canvasWidthPx: Float,
        minDimPx: Float,
        scale: Float
    ): Float = if (decoration.isBottomCaption()) {
        canvasWidthPx * (1f - 2f * API_CAPTION_HORIZONTAL_INSET_RATIO)
    } else {
        minDimPx * TEXT_BOX_RATIO * scale
    }

    fun textBoxHeightPx(
        decoration: TextDecoration,
        minDimPx: Float,
        scale: Float
    ): Float = if (decoration.isBottomCaption()) {
        minDimPx * 0.46f * scale
    } else {
        minDimPx * TEXT_BOX_RATIO * scale
    }

    fun textSizePx(
        decoration: TextDecoration,
        minDimPx: Float,
        scale: Float
    ): Float = minDimPx * if (decoration.isBottomCaption()) {
        API_CAPTION_TEXT_SIZE_RATIO
    } else {
        TEXT_SIZE_RATIO
    } * scale

    fun textMaxLines(decoration: TextDecoration): Int =
        if (decoration.isBottomCaption()) 6 else 3

    fun layerStrokeWidthPx(textSizePx: Float, strokeWidthRatio: Float): Float =
        textSizePx * strokeWidthRatio.coerceIn(0f, 0.2f)

    fun layerOffsetPx(textSizePx: Float, ratio: Float): Float = textSizePx * ratio

    fun shadowBlurRadiusPx(textSizePx: Float, ratio: Float): Float =
        if (ratio <= 0f) 0f else textSizePx * ratio.coerceIn(0f, 0.3f)

    fun textDecorationPaddingPx(resolvedStyle: ResolvedTextDecorationStyle): Float {
        val strokePad = resolvedStyle.layers.maxOfOrNull { layer ->
            if (!layer.isFill) layer.strokeWidthPx else 0f
        } ?: 0f
        val layerOffsetPad = resolvedStyle.layers.maxOfOrNull { layer ->
            kotlin.math.max(layer.offsetXPx, layer.offsetYPx)
        } ?: 0f
        return strokePad * 2f + layerOffsetPad
    }

    fun textDecorationBoxSizePx(
        decoration: TextDecoration,
        canvasWidthPx: Float,
        minDimPx: Float,
        scale: Float,
        measuredTextWidthPx: Float,
        measuredTextHeightPx: Float,
        resolvedStyle: ResolvedTextDecorationStyle
    ): Pair<Float, Float> {
        val baseWidth = textBoxWidthPx(decoration, canvasWidthPx, minDimPx, scale)
        val baseHeight = textBoxHeightPx(decoration, minDimPx, scale)
        val padding = textDecorationPaddingPx(resolvedStyle)
        return kotlin.math.max(baseWidth, measuredTextWidthPx + padding) to
            kotlin.math.max(baseHeight, measuredTextHeightPx + padding)
    }
}

fun TextDecoration.isBottomCaption(): Boolean = layout == TextDecorationLayout.BottomCaption
