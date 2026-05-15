package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalSubtleOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val onSurface = neubrutalOnSurface()
    val shape = RoundedCornerShape(NeubrutalCardRadius)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .neubrutalShadow(
                    offsetX = NeubrutalShadowOffset,
                    offsetY = NeubrutalShadowOffset,
                    cornerRadius = NeubrutalCardRadius,
                    color = neubrutalShadowColor()
                )
                .clip(shape)
                .background(surface)
                .border(
                    width = NeubrutalBorderWidth,
                    color = if (isError) presentation.theme.ErrorRed else border,
                    shape = shape
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = TextStyle(
                color = onSurface,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(onSurface),
            keyboardOptions = KeyboardOptions(
                imeAction = imeAction,
                keyboardType = keyboardType
            ),
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = neubrutalSubtleOnSurface()
                    )
                }
                innerTextField()
            }
        )

        if (isError && supportingText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            supportingText()
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun AppTextFieldPreview() {
    MaterialTheme {
        AppTextField(
            value = "My Sticker Pack",
            onValueChange = {},
            label = stringResource(Res.string.pack_name_label),
            placeholder = stringResource(Res.string.pack_name_placeholder)
        )
    }
}

@Preview
@Composable
private fun AppTextFieldEmptyPreview() {
    MaterialTheme {
        AppTextField(
            value = "",
            onValueChange = {},
            label = stringResource(Res.string.pack_name_label),
            placeholder = stringResource(Res.string.pack_name_placeholder)
        )
    }
}
