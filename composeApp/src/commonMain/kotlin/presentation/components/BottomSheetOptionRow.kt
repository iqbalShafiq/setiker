package presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import presentation.common.ContentStateAnimations
import presentation.common.rememberReduceMotion
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@Composable
fun BottomSheetOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    val haptics = rememberAppHaptics()
    val reduceMotion = rememberReduceMotion()
    var revealed by remember { mutableStateOf(false) }
    val reveal by animateFloatAsState(
        targetValue = if (revealed || reduceMotion) 1f else 0f,
        animationSpec = SetikerMotion.out(SetikerMotion.DURATION_BASE_MS),
        label = "sheet_option_reveal"
    )
    LaunchedEffect(Unit) {
        if (!reduceMotion) delay((index * SetikerMotion.STAGGER_STEP_MS).milliseconds)
        revealed = true
    }
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_REGULAR,
        label = "bottom_sheet_option_row_scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            AccentCoral.copy(alpha = 0.16f)
        } else {
            neubrutalCardSurface()
        },
        animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_FAST_MS),
        label = "sheet_option_background"
    )
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 10.dp.toPx()
            }
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalShadowOffset,
                pressedOffset = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = neubrutalShadowColor()
            )
            .clip(shape)
            .background(backgroundColor)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = selected)
            )
            .selectable(
                selected = selected,
                interactionSource = pressFeedback.interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = {
                    haptics.selection()
                    onClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = neubrutalOnSurface(),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        val checkBackground by animateColorAsState(
            targetValue = if (selected) AccentCoral else neubrutalCardSurface(),
            animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_FAST_MS),
            label = "sheet_option_check_background"
        )
        val checkBorder by animateColorAsState(
            targetValue = if (selected) {
                neubrutalBorderColor()
            } else {
                neubrutalBorderColor().copy(alpha = 0.65f)
            },
            animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_FAST_MS),
            label = "sheet_option_check_border"
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .neubrutalShadow(
                    offsetX = NeubrutalSmallShadowOffset,
                    offsetY = NeubrutalSmallShadowOffset,
                    cornerRadius = NeubrutalSmallRadius,
                    color = if (selected) neubrutalShadowColor() else neubrutalShadowColor().copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(NeubrutalSmallRadius))
                .background(checkBackground)
                .neubrutalBorderWithGloss(
                    color = checkBorder,
                    cornerRadius = NeubrutalSmallRadius,
                    highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = selected)
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { ContentStateAnimations.fadeOnly() },
                label = "sheet_option_check"
            ) { isSelected ->
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(neubrutalMutedOnSurface().copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}
