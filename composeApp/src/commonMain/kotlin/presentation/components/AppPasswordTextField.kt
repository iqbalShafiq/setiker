package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import presentation.icons.SetikerMorphs
import presentation.theme.ErrorRed
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
import presentation.theme.neubrutalFill
import presentation.theme.NeubrutalBorderRole
import presentation.theme.neubrutalBorderFor

@Composable
fun AppPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    /**
     * The reason the field is invalid, in words.
     *
     * A red border is invisible to a screen reader. This is what makes TalkBack say that the
     * field is invalid and what is wrong with it.
     */
    errorText: String? = null,
    supportingText: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neubrutalShadow(
                    offsetX = NeubrutalShadowOffset,
                    offsetY = NeubrutalShadowOffset,
                    cornerRadius = NeubrutalCardRadius,
                    color = if (enabled) {
                        neubrutalShadowColor()
                    } else {
                        neubrutalShadowColor().copy(alpha = 0.4f)
                    }
                )
                .clip(shape)
                .neubrutalFill(color = surface, cornerRadius = NeubrutalCardRadius)
                .neubrutalBorderWithGloss(
                    color = neubrutalBorderFor(
                        when {
                            !enabled -> NeubrutalBorderRole.Disabled
                            isError -> NeubrutalBorderRole.Error
                            else -> NeubrutalBorderRole.Container
                        }
                    ),
                    cornerRadius = NeubrutalCardRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { if (enabled) onValueChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 24.dp)
                    .then(
                        // A red border is invisible to a screen reader. This is what makes
                        // TalkBack say that the field is invalid and what is wrong with it.
                        if (isError && errorText != null) {
                            Modifier.semantics { error(errorText) }
                        } else {
                            Modifier
                        }
                    ),
                singleLine = true,
                maxLines = 1,
                enabled = enabled,
                textStyle = TextStyle(
                    color = if (enabled) onSurface else onSurface.copy(alpha = 0.45f),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(onSurface),
                keyboardOptions = KeyboardOptions(
                    imeAction = imeAction,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (!focusManager.moveFocus(FocusDirection.Down)) {
                            focusManager.clearFocus()
                        }
                    },
                    onDone = {
                        focusManager.clearFocus()
                        onDone?.invoke()
                    },
                    onGo = {
                        focusManager.clearFocus()
                        onDone?.invoke()
                    },
                    onSend = {
                        focusManager.clearFocus()
                        onDone?.invoke()
                    },
                    onSearch = {
                        focusManager.clearFocus()
                        onDone?.invoke()
                    }
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = neubrutalSubtleOnSurface(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    innerTextField()
                }
            )

            val haptics = rememberAppHaptics()
            val visibilityFeedback = rememberPressFeedback(
                pressedScale = SetikerMotion.PRESS_SCALE_TINY,
                enabled = enabled,
                label = "password_visibility_scale"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(20.dp)
                    .pressScale(visibilityFeedback)
                    .clickableNoRipple(
                        interactionSource = visibilityFeedback.interactionSource,
                        enabled = enabled,
                        onClick = {
                            haptics.selection()
                            passwordVisible = !passwordVisible
                        }
                    )
                    .semantics {
                        contentDescription =
                            if (passwordVisible) "Hide password" else "Show password"
                    }
            ) {
                MorphGlyphIcon(
                    morph = SetikerMorphs.Eye,
                    checked = passwordVisible,
                    size = 20.dp,
                    tint = neubrutalSubtleOnSurface(),
                    enabled = enabled
                )
            }
        }

        if (isError && supportingText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            supportingText()
        }
    }
}

// MARK: - Previews
@Preview
@Composable
private fun AppPasswordTextFieldPreview() {
    MaterialTheme {
        AppPasswordTextField(
            value = "",
            onValueChange = {},
            label = "Password",
            placeholder = "Enter your password"
        )
    }
}



