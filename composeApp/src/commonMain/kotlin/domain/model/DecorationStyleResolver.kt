package domain.model

data class ResolvedTextDecorationStyle(
    val font: DecorationFont,
    val fontWeight: DecorationFontWeight,
    val textColorArgb: Long,
    val layers: List<ResolvedTextEffectLayer>
)

data class ResolvedTextEffectLayer(
    val colorArgb: Long,
    val strokeWidthPx: Float,
    val offsetXPx: Float,
    val offsetYPx: Float,
    val shadowBlurPx: Float,
    val shadowOffsetYPx: Float,
    val isFill: Boolean,
    val gradientStartArgb: Long? = null,
    val gradientEndArgb: Long? = null
)

fun TextDecoration.resolveStyle(textSizePx: Float): ResolvedTextDecorationStyle {
    if (style == TextDecorationStyle.Custom) {
        return resolveCustomStyle(textSizePx)
    }

    val preset = TextDecorationStyleRegistry.preset(style)
    return ResolvedTextDecorationStyle(
        font = preset.font,
        fontWeight = preset.fontWeight,
        textColorArgb = textColorArgb,
        layers = preset.layers.map { it.resolve(textSizePx, textColorArgb) }
    )
}

private fun TextDecoration.resolveCustomStyle(textSizePx: Float): ResolvedTextDecorationStyle {
    val strokeWidthPx = DecorationRenderSpec.layerStrokeWidthPx(textSizePx, borderWidthRatio)
    val layers = buildList {
        if (strokeWidthPx > 0f) {
            add(
                ResolvedTextEffectLayer(
                    colorArgb = borderColorArgb,
                    strokeWidthPx = strokeWidthPx,
                    offsetXPx = 0f,
                    offsetYPx = 0f,
                    shadowBlurPx = 0f,
                    shadowOffsetYPx = 0f,
                    isFill = false,
                    gradientStartArgb = null,
                    gradientEndArgb = null
                )
            )
        }
        add(
            ResolvedTextEffectLayer(
                colorArgb = textColorArgb,
                strokeWidthPx = 0f,
                offsetXPx = 0f,
                offsetYPx = 0f,
                shadowBlurPx = 0f,
                shadowOffsetYPx = 0f,
                isFill = true,
                gradientStartArgb = null,
                gradientEndArgb = null
            )
        )
    }
    return ResolvedTextDecorationStyle(
        font = font,
        fontWeight = fontWeight,
        textColorArgb = textColorArgb,
        layers = layers
    )
}

private fun TextEffectLayer.resolve(textSizePx: Float, fillColorArgb: Long): ResolvedTextEffectLayer {
    val color = if (useFillColor) fillColorArgb else colorArgb
    return ResolvedTextEffectLayer(
        colorArgb = color,
        strokeWidthPx = strokeWidthRatio?.let { DecorationRenderSpec.layerStrokeWidthPx(textSizePx, it) } ?: 0f,
        offsetXPx = DecorationRenderSpec.layerOffsetPx(textSizePx, offsetXRatio),
        offsetYPx = DecorationRenderSpec.layerOffsetPx(textSizePx, offsetYRatio),
        shadowBlurPx = DecorationRenderSpec.shadowBlurRadiusPx(textSizePx, shadowBlurRatio),
        shadowOffsetYPx = DecorationRenderSpec.layerOffsetPx(textSizePx, shadowOffsetYRatio),
        isFill = strokeWidthRatio == null,
        gradientStartArgb = gradientStartArgb,
        gradientEndArgb = gradientEndArgb
    )
}

fun TextDecoration.inferStyleFromLegacyFields(): TextDecorationStyle {
    if (style != TextDecorationStyle.ClassicOutline) return style
    if (source == TextDecorationSource.ApiTextAsset || source == TextDecorationSource.ApiOutsideForeground) {
        return TextDecorationStyle.ApiCaption
    }
    val classic = TextDecorationStyleRegistry.preset(TextDecorationStyle.ClassicOutline)
    val borderDiffers = borderColorArgb != DEFAULT_DECORATION_BORDER_COLOR_ARGB ||
        borderWidthRatio != DEFAULT_DECORATION_BORDER_WIDTH_RATIO
    val fontDiffers = font != classic.font || fontWeight != classic.fontWeight
    return if (borderDiffers || fontDiffers) TextDecorationStyle.Custom else TextDecorationStyle.ClassicOutline
}

fun EmojiDecoration.resolveEmojiStyle(textSizePx: Float): ResolvedTextDecorationStyle {
    val strokeWidthPx = DecorationRenderSpec.layerStrokeWidthPx(textSizePx, borderWidthRatio)
    val layers = buildList {
        if (strokeWidthPx > 0f) {
            add(
                ResolvedTextEffectLayer(
                    colorArgb = borderColorArgb,
                    strokeWidthPx = strokeWidthPx,
                    offsetXPx = 0f,
                    offsetYPx = 0f,
                    shadowBlurPx = 0f,
                    shadowOffsetYPx = 0f,
                    isFill = false
                )
            )
        }
        add(
            ResolvedTextEffectLayer(
                colorArgb = 0xFFFFFFFFL,
                strokeWidthPx = 0f,
                offsetXPx = 0f,
                offsetYPx = 0f,
                shadowBlurPx = 0f,
                shadowOffsetYPx = 0f,
                isFill = true
            )
        )
    }
    return ResolvedTextDecorationStyle(
        font = DecorationFont.Sans,
        fontWeight = DecorationFontWeight.Regular,
        textColorArgb = 0xFFFFFFFFL,
        layers = layers
    )
}

fun TextDecoration.applyStylePreset(
    newStyle: TextDecorationStyle,
    preserveTextColor: Boolean = true
): TextDecoration {
    if (newStyle == TextDecorationStyle.Custom) return copy(style = newStyle)
    val preset = TextDecorationStyleRegistry.preset(newStyle)
    return copy(
        style = newStyle,
        font = preset.font,
        fontWeight = preset.fontWeight,
        textColorArgb = if (preserveTextColor) textColorArgb else preset.defaultTextColorArgb,
        borderColorArgb = preset.layers.firstOrNull { it.strokeWidthRatio != null }?.colorArgb
            ?: DEFAULT_DECORATION_BORDER_COLOR_ARGB,
        borderWidthRatio = preset.layers.firstOrNull { it.strokeWidthRatio != null }?.strokeWidthRatio
            ?: DEFAULT_DECORATION_BORDER_WIDTH_RATIO
    )
}
