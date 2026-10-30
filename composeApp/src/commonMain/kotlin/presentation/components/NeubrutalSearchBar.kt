package presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.clear
import setiker.composeapp.generated.resources.search_packs_placeholder

@Composable
fun NeubrutalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = stringResource(Res.string.search_packs_placeholder),
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()
    val onSurface = neubrutalOnSurface()
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    // A focus ring that fades in: the search field used to give no sign of being focused beyond
    // the blinking cursor.
    val focusRing by animateColorAsState(
        targetValue = if (isFocused) AccentCoral else Color.Transparent,
        animationSpec = SetikerMotion.standard(),
        label = "search_bar_focus_ring"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = shadow
            )
            .clip(shape)
            .background(surface)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .border(width = 2.dp, color = focusRing, shape = shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = neubrutalSubtleOnSurface()
        )

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            interactionSource = interactionSource,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = onSurface,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(onSurface),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = neubrutalSubtleOnSurface()
                    )
                }
                innerTextField()
            }
        )

        if (query.isNotEmpty()) {
            // 48 dp touch target with a 20 dp glyph: the old 32 dp box was well below the minimum
            // touch target, and it kept the Material ripple instead of the app's press language.
            NeubrutalIconButton(
                icon = Icons.Default.Clear,
                contentDescription = stringResource(Res.string.clear),
                onClick = { onQueryChange("") },
                iconSize = 20.dp,
                tint = neubrutalSubtleOnSurface(),
                pressedScale = 0.88f,
                label = "search_clear_scale"
            )
        }
    }
}

// MARK: - Preview

@Preview
@Composable
private fun NeubrutalSearchBarPreview() {
    MaterialTheme {
        NeubrutalSearchBar(
            query = "",
            onQueryChange = {}
        )
    }
}

@Preview
@Composable
private fun NeubrutalSearchBarWithTextPreview() {
    MaterialTheme {
        NeubrutalSearchBar(
            query = "Cats",
            onQueryChange = {}
        )
    }
}
