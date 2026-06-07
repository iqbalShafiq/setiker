package domain.model

object TextDecorationStyleRegistry {

    fun preset(style: TextDecorationStyle): TextDecorationStylePreset = when (style) {
        TextDecorationStyle.ClassicOutline -> classicOutline()
        TextDecorationStyle.StickerPop -> stickerPop()
        TextDecorationStyle.BubbleRed -> bubbleRed()
        TextDecorationStyle.NeonCyan -> neonCyan()
        TextDecorationStyle.Minimal -> minimal()
        TextDecorationStyle.SunsetGradient -> sunsetGradient()
        TextDecorationStyle.ApiCaption -> apiCaption()
        TextDecorationStyle.Custom -> classicOutline()
    }

    val selectableStyles: List<TextDecorationStyle> = listOf(
        TextDecorationStyle.ClassicOutline,
        TextDecorationStyle.StickerPop,
        TextDecorationStyle.BubbleRed,
        TextDecorationStyle.NeonCyan,
        TextDecorationStyle.Minimal,
        TextDecorationStyle.SunsetGradient
    )

    private fun classicOutline(): TextDecorationStylePreset = TextDecorationStylePreset(
        style = TextDecorationStyle.ClassicOutline,
        font = DecorationFont.Fredoka,
        fontWeight = DecorationFontWeight.Bold,
        defaultTextColorArgb = 0xFF000000L,
        layers = listOf(
            TextEffectLayer(colorArgb = 0xFFFFFFFFL, strokeWidthRatio = 0.10f),
            TextEffectLayer(colorArgb = 0xFF000000L, useFillColor = true)
        )
    )

    private fun stickerPop(): TextDecorationStylePreset = TextDecorationStylePreset(
        style = TextDecorationStyle.StickerPop,
        font = DecorationFont.Bungee,
        fontWeight = DecorationFontWeight.Regular,
        defaultTextColorArgb = 0xFFFFE600L,
        layers = listOf(
            TextEffectLayer(
                colorArgb = 0xFF000000L,
                offsetXRatio = 0.04f,
                offsetYRatio = 0.06f
            ),
            TextEffectLayer(colorArgb = 0xFFFF1493L, strokeWidthRatio = 0.12f),
            TextEffectLayer(colorArgb = 0xFFFFFFFFL, strokeWidthRatio = 0.06f),
            TextEffectLayer(colorArgb = 0xFFFFE600L, useFillColor = true)
        )
    )

    private fun bubbleRed(): TextDecorationStylePreset = TextDecorationStylePreset(
        style = TextDecorationStyle.BubbleRed,
        font = DecorationFont.LuckiestGuy,
        fontWeight = DecorationFontWeight.Regular,
        defaultTextColorArgb = 0xFFE53935L,
        layers = listOf(
            TextEffectLayer(
                colorArgb = 0xFF1565C0L,
                offsetXRatio = 0.03f,
                offsetYRatio = 0.05f
            ),
            TextEffectLayer(colorArgb = 0xFF1565C0L, strokeWidthRatio = 0.14f),
            TextEffectLayer(colorArgb = 0xFFFFFFFFL, strokeWidthRatio = 0.08f),
            TextEffectLayer(colorArgb = 0xFFE53935L, useFillColor = true)
        )
    )

    private fun neonCyan(): TextDecorationStylePreset = TextDecorationStylePreset(
        style = TextDecorationStyle.NeonCyan,
        font = DecorationFont.Bungee,
        fontWeight = DecorationFontWeight.Regular,
        defaultTextColorArgb = 0xFF00E5FFL,
        layers = listOf(
            TextEffectLayer(
                colorArgb = 0xFF000000L,
                offsetXRatio = 0.04f,
                offsetYRatio = 0.05f,
                shadowBlurRatio = 0.06f,
                shadowOffsetYRatio = 0.02f
            ),
            TextEffectLayer(colorArgb = 0xFFFFFFFFL, strokeWidthRatio = 0.08f),
            TextEffectLayer(colorArgb = 0xFF00E5FFL, useFillColor = true)
        )
    )

    private fun minimal(): TextDecorationStylePreset = TextDecorationStylePreset(
        style = TextDecorationStyle.Minimal,
        font = DecorationFont.Fredoka,
        fontWeight = DecorationFontWeight.Medium,
        defaultTextColorArgb = 0xFFFFFFFFL,
        layers = listOf(
            TextEffectLayer(colorArgb = 0xFF000000L, strokeWidthRatio = 0.05f),
            TextEffectLayer(colorArgb = 0xFFFFFFFFL, useFillColor = true)
        )
    )

    private fun sunsetGradient(): TextDecorationStylePreset = TextDecorationStylePreset(
        style = TextDecorationStyle.SunsetGradient,
        font = DecorationFont.Bungee,
        fontWeight = DecorationFontWeight.Regular,
        defaultTextColorArgb = 0xFFFF6B35L,
        layers = listOf(
            TextEffectLayer(
                colorArgb = 0xFF000000L,
                offsetXRatio = 0.03f,
                offsetYRatio = 0.05f
            ),
            TextEffectLayer(colorArgb = 0xFFFFFFFFL, strokeWidthRatio = 0.07f),
            TextEffectLayer(
                colorArgb = 0xFFFF6B35L,
                useFillColor = true,
                gradientStartArgb = 0xFFFFD700L,
                gradientEndArgb = 0xFFFF1493L
            )
        )
    )

    private fun apiCaption(): TextDecorationStylePreset = TextDecorationStylePreset(
        style = TextDecorationStyle.ApiCaption,
        font = DecorationFont.Fredoka,
        fontWeight = DecorationFontWeight.SemiBold,
        defaultTextColorArgb = 0xFF000000L,
        layers = listOf(
            TextEffectLayer(
                colorArgb = 0x88000000L,
                shadowBlurRatio = 0.14f,
                shadowOffsetYRatio = 0.02f
            ),
            TextEffectLayer(colorArgb = 0xFFFFFFFFL, strokeWidthRatio = 0.06f),
            TextEffectLayer(colorArgb = 0xFF000000L, useFillColor = true)
        )
    )
}
