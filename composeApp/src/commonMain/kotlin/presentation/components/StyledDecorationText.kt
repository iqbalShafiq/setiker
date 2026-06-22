package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.model.ResolvedTextDecorationStyle
import domain.model.ResolvedTextEffectLayer
import domain.model.TextDecoration
import domain.model.TextDecorationStyle
import domain.model.TextDecorationStyleRegistry
import domain.model.resolveStyle
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
fun StyledDecorationText(
    text: String,
    resolvedStyle: ResolvedTextDecorationStyle,
    baseStyle: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        resolvedStyle.layers.forEach { layer ->
            DecorationTextLayer(
                text = text,
                layer = layer,
                baseStyle = baseStyle,
                maxLines = maxLines
            )
        }
    }
}

@Composable
private fun DecorationTextLayer(
    text: String,
    layer: ResolvedTextEffectLayer,
    baseStyle: TextStyle,
    maxLines: Int
) {
    val layerStyle = decorationLayerTextStyle(layer, baseStyle)

    val offsetModifier = if (layer.offsetXPx != 0f || layer.offsetYPx != 0f) {
        Modifier.offset {
            IntOffset(layer.offsetXPx.roundToInt(), layer.offsetYPx.roundToInt())
        }
    } else {
        Modifier
    }

    Text(
        text = text,
        style = layerStyle,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        modifier = Modifier
            .wrapContentSize()
            .then(offsetModifier)
    )
}

@Preview
@Composable
private fun StyledDecorationTextClassicPreview() {
    StyledDecorationTextPresetPreview(TextDecorationStyle.ClassicOutline)
}

@Preview
@Composable
private fun StyledDecorationTextPopPreview() {
    StyledDecorationTextPresetPreview(TextDecorationStyle.StickerPop)
}

@Preview
@Composable
private fun StyledDecorationTextBubblePreview() {
    StyledDecorationTextPresetPreview(TextDecorationStyle.BubbleRed)
}

@Preview
@Composable
private fun StyledDecorationTextNeonPreview() {
    StyledDecorationTextPresetPreview(TextDecorationStyle.NeonCyan)
}

@Preview
@Composable
private fun StyledDecorationTextMinimalPreview() {
    StyledDecorationTextPresetPreview(TextDecorationStyle.Minimal)
}

@Preview
@Composable
private fun StyledDecorationTextSunsetPreview() {
    StyledDecorationTextPresetPreview(TextDecorationStyle.SunsetGradient)
}

@Composable
private fun StyledDecorationTextPresetPreview(style: TextDecorationStyle) {
    val preset = TextDecorationStyleRegistry.preset(style)
    val previewTextSizePx = 28f
    val resolved = TextDecoration(
        id = "preview",
        text = "Aa",
        font = preset.font,
        fontWeight = preset.fontWeight,
        textColorArgb = preset.defaultTextColorArgb,
        style = style
    ).resolveStyle(previewTextSizePx)

    MaterialTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFFE8E0D5))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StyledDecorationText(
                text = "Sticker",
                resolvedStyle = resolved,
                baseStyle = TextStyle(
                    fontFamily = decorationFontFamily(preset.font, preset.fontWeight),
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}
