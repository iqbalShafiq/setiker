package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.TextDecorationLayout
import domain.model.TextDecorationStyle
import org.jetbrains.compose.resources.stringResource
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlight
import presentation.theme.neubrutalGlossyHighlightColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.add_decoration
import setiker.composeapp.generated.resources.add_text
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.change_border_thickness
import setiker.composeapp.generated.resources.edit_text_decoration
import setiker.composeapp.generated.resources.text_decoration
import setiker.composeapp.generated.resources.text_decoration_placeholder

/**
 * Shared decoration bottom sheets used by both static (`EditorScreen`) and animated
 * (`AnimatedEditorScreen`) sticker editors. Extracting these here ensures both editors offer
 * the exact same UX when a decoration is selected and reduces drift.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTextDecorationBottomSheet(
    onAdd: (text: String, style: TextDecorationStyle) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(TextDecorationStyle.ClassicOutline) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(Res.string.text_decoration),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(Res.string.add_text),
                placeholder = stringResource(Res.string.text_decoration_placeholder)
            )
            Spacer(modifier = Modifier.height(12.dp))
            StylePresetChipRow(selectedStyle = selectedStyle, onSelect = { selectedStyle = it })
            Spacer(modifier = Modifier.height(16.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.add_decoration),
                enabled = text.isNotBlank(),
                onClick = { onAdd(text, selectedStyle) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppSecondaryButton(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextDecorationBottomSheet(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(Res.string.edit_text_decoration),
                placeholder = stringResource(Res.string.text_decoration_placeholder)
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.add_decoration),
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StylePickerBottomSheet(
    previewText: String,
    selectedStyle: TextDecorationStyle,
    selectedFont: DecorationFont,
    selectedWeight: DecorationFontWeight,
    selectedLayout: TextDecorationLayout,
    arcIntensity: Float,
    borderColorArgb: Long,
    borderWidthRatio: Float,
    onSelectStyle: (TextDecorationStyle) -> Unit,
    onSelectFont: (DecorationFont) -> Unit,
    onSelectWeight: (DecorationFontWeight) -> Unit,
    onSelectLayout: (TextDecorationLayout) -> Unit,
    onArcIntensityChange: (Float) -> Unit,
    onBorderColorChange: (Long) -> Unit,
    onBorderWidthChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                DecorationTextContent(
                    decoration = domain.model.TextDecoration(
                        id = "preview",
                        text = previewText.ifBlank { "Sample" },
                        font = selectedFont,
                        fontWeight = selectedWeight,
                        borderColorArgb = borderColorArgb,
                        borderWidthRatio = borderWidthRatio,
                        style = selectedStyle,
                        layout = selectedLayout,
                        arcIntensity = arcIntensity
                    ),
                    minDim = 512f,
                    scale = 1f
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            StylePresetChipRow(selectedStyle = selectedStyle, onSelect = onSelectStyle)
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(TextDecorationLayout.Freeform, TextDecorationLayout.Arched).forEach { layout ->
                    DecorationSheetFilterChip(
                        selected = selectedLayout == layout,
                        onClick = { onSelectLayout(layout) },
                        label = {
                            Text(
                                if (layout == TextDecorationLayout.Arched) "Arched" else "Flat",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    )
                }
            }
            if (selectedLayout == TextDecorationLayout.Arched) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Arc ${(arcIntensity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = neubrutalOnSurface()
                )
                Slider(
                    value = arcIntensity,
                    onValueChange = onArcIntensityChange,
                    valueRange = -1f..1f
                )
            }
            if (selectedStyle == TextDecorationStyle.Custom) {
                Spacer(modifier = Modifier.height(12.dp))
                FontChipRow(selectedFont = selectedFont, onSelect = onSelectFont)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DecorationFontWeight.entries.forEach { weight ->
                        DecorationSheetFilterChip(
                            selected = selectedWeight == weight,
                            onClick = { onSelectWeight(weight) },
                            label = { Text(weight.name, fontWeight = mapFontWeight(weight)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Border ${(borderWidthRatio * 100f).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = neubrutalOnSurface()
                )
                Slider(
                    value = borderWidthRatio,
                    onValueChange = onBorderWidthChange,
                    valueRange = 0f..0.2f
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppSecondaryButton(
                    text = "Use white border",
                    onClick = { onBorderColorChange(0xFFFFFFFFL) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FontPickerBottomSheet(
    selectedFont: DecorationFont,
    selectedWeight: DecorationFontWeight,
    onSelectFont: (DecorationFont) -> Unit,
    onSelectWeight: (DecorationFontWeight) -> Unit,
    onDismiss: () -> Unit
) {
    StylePickerBottomSheet(
        previewText = "Aa Bb",
        selectedStyle = TextDecorationStyle.Custom,
        selectedFont = selectedFont,
        selectedWeight = selectedWeight,
        selectedLayout = TextDecorationLayout.Freeform,
        arcIntensity = 0f,
        borderColorArgb = 0xFFFFFFFFL,
        borderWidthRatio = 0.08f,
        onSelectStyle = {},
        onSelectFont = onSelectFont,
        onSelectWeight = onSelectWeight,
        onSelectLayout = {},
        onArcIntensityChange = {},
        onBorderColorChange = {},
        onBorderWidthChange = {},
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    selectedColorArgb: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialHsv = remember(selectedColorArgb) { argbToHsv(selectedColorArgb.toInt()) }
    var hue by remember(selectedColorArgb) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(selectedColorArgb) { mutableStateOf(initialHsv[1]) }
    var value by remember(selectedColorArgb) { mutableStateOf(initialHsv[2]) }
    val selectedColor = Color.hsv(hue = hue, saturation = saturation, value = value)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Color preview",
                style = MaterialTheme.typography.labelLarge,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(NeubrutalSmallRadius))
                    .background(selectedColor)
                    .neubrutalBorderWithGloss(
                        color = neubrutalBorderColor(),
                        cornerRadius = NeubrutalSmallRadius,
                        highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
                    )
            )
            Spacer(modifier = Modifier.height(16.dp))

            ColorSliderRow(
                label = "Hue",
                value = hue,
                valueRange = 0f..360f,
                trackBrush = Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                ),
                onValueChange = { hue = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorSliderRow(
                label = "Saturation",
                value = saturation,
                valueRange = 0f..1f,
                trackBrush = Brush.horizontalGradient(
                    listOf(
                        Color.hsv(hue, 0f, value),
                        Color.hsv(hue, 1f, value)
                    )
                ),
                onValueChange = { saturation = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorSliderRow(
                label = "Brightness",
                value = value,
                valueRange = 0f..1f,
                trackBrush = Brush.horizontalGradient(
                    listOf(
                        Color.Black,
                        Color.hsv(hue, saturation, 1f)
                    )
                ),
                onValueChange = { value = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.add_decoration),
                onClick = { onSelect(selectedColor.toArgb().toLong() and 0xFFFFFFFFL) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorderStyleBottomSheet(
    initialBorderColorArgb: Long,
    initialWidthRatio: Float,
    onSelect: (Long, Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialHsv = remember(initialBorderColorArgb) { argbToHsv(initialBorderColorArgb.toInt()) }
    var hue by remember(initialBorderColorArgb) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(initialBorderColorArgb) { mutableStateOf(initialHsv[1]) }
    var value by remember(initialBorderColorArgb) { mutableStateOf(initialHsv[2]) }
    val selectedColor = Color.hsv(hue = hue, saturation = saturation, value = value)
    var widthRatio by remember(initialWidthRatio) { mutableStateOf(initialWidthRatio.coerceIn(0f, 0.2f)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(Res.string.change_border_thickness),
                style = MaterialTheme.typography.titleMedium,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(NeubrutalSmallRadius))
                    .background(selectedColor)
                    .neubrutalBorderWithGloss(
                        color = neubrutalBorderColor(),
                        cornerRadius = NeubrutalSmallRadius,
                        highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
                    )
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorSliderRow(
                label = "Hue",
                value = hue,
                valueRange = 0f..360f,
                trackBrush = Brush.horizontalGradient(
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                ),
                onValueChange = { hue = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorSliderRow(
                label = "Saturation",
                value = saturation,
                valueRange = 0f..1f,
                trackBrush = Brush.horizontalGradient(
                    listOf(Color.hsv(hue, 0f, value), Color.hsv(hue, 1f, value))
                ),
                onValueChange = { saturation = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorSliderRow(
                label = "Brightness",
                value = value,
                valueRange = 0f..1f,
                trackBrush = Brush.horizontalGradient(
                    listOf(Color.Black, Color.hsv(hue, saturation, 1f))
                ),
                onValueChange = { value = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${(widthRatio * 100f).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = neubrutalOnSurface()
            )
            Slider(
                value = widthRatio,
                onValueChange = { widthRatio = it },
                valueRange = 0f..0.2f
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.add_decoration),
                onClick = { onSelect(selectedColor.toArgb().toLong() and 0xFFFFFFFFL, widthRatio) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontChipRow(
    selectedFont: DecorationFont,
    onSelect: (DecorationFont) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DecorationFont.entries.forEach { font ->
            DecorationSheetFilterChip(
                selected = selectedFont == font,
                onClick = { onSelect(font) },
                label = { Text(font.name, fontFamily = mapFontFamily(font)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecorationSheetFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = neubrutalCardSurface(),
            labelColor = neubrutalOnSurface(),
            selectedContainerColor = primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = neubrutalBorderColor(),
            selectedBorderColor = primary,
            disabledBorderColor = neubrutalBorderColor()
        )
    )
}

@Composable
private fun ColorSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    trackBrush: Brush,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = neubrutalOnSurface()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(trackBrush)
                .border(1.dp, neubrutalBorderColor(), RoundedCornerShape(999.dp))
                .neubrutalGlossyHighlight(
                    cornerRadius = 999.dp,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

private fun argbToHsv(argb: Int): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta).let { if (it < 0f) it + 6f else it } * 60f
        max == g -> (((b - r) / delta) + 2f) * 60f
        else -> (((r - g) / delta) + 4f) * 60f
    }
    val saturation = if (max == 0f) 0f else delta / max
    val value = max
    return floatArrayOf(hue, saturation, value)
}

internal fun mapFontFamily(font: DecorationFont): FontFamily = when (font) {
    DecorationFont.Bungee,
    DecorationFont.LuckiestGuy,
    DecorationFont.Fredoka -> FontFamily.SansSerif
    DecorationFont.Sans -> FontFamily.SansSerif
    DecorationFont.Serif -> FontFamily.Serif
    DecorationFont.Mono -> FontFamily.Monospace
    DecorationFont.Cursive -> FontFamily.Cursive
    DecorationFont.Display -> FontFamily.Serif
    DecorationFont.Rounded -> FontFamily.SansSerif
    DecorationFont.Condensed -> FontFamily.SansSerif
}

internal fun mapFontWeight(weight: DecorationFontWeight): FontWeight = when (weight) {
    DecorationFontWeight.Light -> FontWeight.Light
    DecorationFontWeight.Regular -> FontWeight.Normal
    DecorationFontWeight.Medium -> FontWeight.Medium
    DecorationFontWeight.SemiBold -> FontWeight.SemiBold
    DecorationFontWeight.Bold -> FontWeight.Bold
}

// MARK: - Previews

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AddTextDecorationBottomSheetPreview() {
    MaterialTheme {
        AddTextDecorationBottomSheet(
            onAdd = { _, _ -> },
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun EditTextDecorationBottomSheetPreview() {
    MaterialTheme {
        EditTextDecorationBottomSheet(
            initialText = "Hello",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview
@Composable
private fun FontPickerBottomSheetPreview() {
    MaterialTheme {
        FontPickerBottomSheet(
            selectedFont = DecorationFont.Sans,
            selectedWeight = DecorationFontWeight.Regular,
            onSelectFont = {},
            onSelectWeight = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview
@Composable
private fun ColorPickerBottomSheetPreview() {
    MaterialTheme {
        ColorPickerBottomSheet(
            selectedColorArgb = 0xFFFF0000L,
            onSelect = {},
            onDismiss = {}
        )
    }
}
