package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.model.TextDecorationStyle
import domain.model.TextDecorationStyleRegistry
import domain.model.resolveStyle
import org.jetbrains.compose.resources.stringResource
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.style_bubble
import setiker.composeapp.generated.resources.style_caption
import setiker.composeapp.generated.resources.style_classic
import setiker.composeapp.generated.resources.style_custom
import setiker.composeapp.generated.resources.style_minimal
import setiker.composeapp.generated.resources.style_neon
import setiker.composeapp.generated.resources.style_pop
import setiker.composeapp.generated.resources.style_sunset

@Composable
fun stylePresetLabel(style: TextDecorationStyle): String = when (style) {
    TextDecorationStyle.ClassicOutline -> stringResource(Res.string.style_classic)
    TextDecorationStyle.StickerPop -> stringResource(Res.string.style_pop)
    TextDecorationStyle.BubbleRed -> stringResource(Res.string.style_bubble)
    TextDecorationStyle.NeonCyan -> stringResource(Res.string.style_neon)
    TextDecorationStyle.Minimal -> stringResource(Res.string.style_minimal)
    TextDecorationStyle.SunsetGradient -> stringResource(Res.string.style_sunset)
    TextDecorationStyle.ApiCaption -> stringResource(Res.string.style_caption)
    TextDecorationStyle.Custom -> stringResource(Res.string.style_custom)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StylePresetChipRow(
    selectedStyle: TextDecorationStyle,
    onSelect: (TextDecorationStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextDecorationStyleRegistry.selectableStyles.forEach { style ->
            StylePresetChip(
                style = style,
                selected = selectedStyle == style,
                onClick = { onSelect(style) }
            )
        }
    }
}

@Composable
fun StylePresetChip(
    style: TextDecorationStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = TextDecorationStyleRegistry.preset(style)
    val previewTextSizePx = 18f
    val resolved = preset.let {
        domain.model.TextDecoration(
            id = "preview",
            text = "Aa",
            font = it.font,
            fontWeight = it.fontWeight,
            textColorArgb = it.defaultTextColorArgb,
            style = style
        ).resolveStyle(previewTextSizePx)
    }

    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .width(76.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(NeubrutalSmallRadius))
            .background(neubrutalScreenBackground())
            .border(
                width = if (selected) NeubrutalBorderWidth * 1.5f else NeubrutalBorderWidth,
                color = if (selected) primary else neubrutalBorderColor(),
                shape = RoundedCornerShape(NeubrutalSmallRadius)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        StyledDecorationText(
            text = "Aa",
            resolvedStyle = resolved,
            baseStyle = TextStyle(
                fontFamily = decorationFontFamily(preset.font, preset.fontWeight),
                fontWeight = mapDecorationFontWeight(preset.fontWeight),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
        Text(
            text = stylePresetLabel(style),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(neubrutalScreenBackground().copy(alpha = 0.85f))
                .padding(horizontal = 4.dp)
        )
    }
}
