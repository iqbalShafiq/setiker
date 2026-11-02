package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import domain.model.aijob.AiJob
import domain.model.aijob.WorkspaceDraft
import org.jetbrains.compose.resources.stringResource
import presentation.aijobs.AiJobProgressMode
import presentation.aijobs.shouldShowFailureDetail
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.ai_jobs_cancel
import setiker.composeapp.generated.resources.ai_jobs_delete
import setiker.composeapp.generated.resources.ai_jobs_open
import setiker.composeapp.generated.resources.ai_jobs_retry

@Composable
fun DraftJobListCard(
    draft: WorkspaceDraft,
    latestJob: AiJob?,
    statusLabel: String,
    progressMode: AiJobProgressMode,
    canRetry: Boolean,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_SUBTLE,
        label = "draft_job_card_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalShadowOffset,
                pressedOffset = NeubrutalSmallShadowOffset,
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
            .clickableNoRipple(
                interactionSource = pressFeedback.interactionSource,
                onClick = onOpen
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = draft.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (progressMode == AiJobProgressMode.DETERMINATE) {
            latestJob?.progress?.let { progress ->
                NeubrutalProgress(
                    progress = progress.fraction.coerceIn(0f, 1f),
                    label = progress.stepLabel.takeIf { it.isNotBlank() },
                    showPercentage = true
                )
            }
        } else if (progressMode == AiJobProgressMode.INDETERMINATE) {
            // The job is running but the server has no progress payload yet. Keep an
            // indeterminate bar on screen so the user still sees that work is happening.
            NeubrutalProgressBar(progress = null)
        }

        latestJob?.failureMessage
            ?.takeIf { shouldShowFailureDetail(statusLabel, it, progressMode) }
            ?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = neubrutalMutedOnSurface()
                )
            }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canRetry) {
                PackBottomBarIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = stringResource(Res.string.ai_jobs_retry),
                    onClick = onRetry
                )
            }
            onCancel?.let { cancel ->
                PackBottomBarIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.ai_jobs_cancel),
                    onClick = cancel
                )
            }
            PackBottomBarIconButton(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(Res.string.ai_jobs_open),
                onClick = onOpen
            )
            PackBottomBarIconButton(
                icon = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.ai_jobs_delete),
                onClick = onDelete
            )
        }
    }
}
