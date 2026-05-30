package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearProgressIndicator
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
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
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
    showProgress: Boolean,
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

    Column(
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
            .border(width = NeubrutalBorderWidth, color = border, shape = shape)
            .clickable(onClick = onOpen)
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

        if (showProgress) {
            latestJob?.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress.fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (progress.stepLabel.isNotBlank()) {
                    Text(
                        text = progress.stepLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = neubrutalMutedOnSurface()
                    )
                }
            }
        }

        latestJob?.failureMessage?.takeIf { !showProgress }?.let { message ->
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
