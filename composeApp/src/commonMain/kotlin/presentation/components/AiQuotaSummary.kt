package presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import domain.model.AiQuotaOperation
import domain.model.AiUsage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import presentation.theme.neubrutalMutedOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.ai_quota_loading
import setiker.composeapp.generated.resources.ai_quota_unavailable
import kotlin.math.roundToInt

@Composable
fun AiQuotaSummary(
    usage: AiUsage?,
    isLoading: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier,
    showOperationCosts: Boolean = false,
    highlightOperation: AiQuotaOperation? = null,
    showIllustration: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when {
            isLoading -> {
                if (showIllustration) {
                    AppIllustrationImage(
                        illustration = AppIllustration.LoadingState,
                        modifier = Modifier.height(140.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AiQuotaLine(text = stringResource(Res.string.ai_quota_loading))
            }
            hasError -> {
                if (showIllustration) {
                    AppIllustrationImage(
                        illustration = AppIllustration.ErrorState,
                        modifier = Modifier.height(140.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AiQuotaLine(text = stringResource(Res.string.ai_quota_unavailable))
            }
            usage != null -> {
                AiQuotaLine(text = usage.compactQuotaText())
                if (showOperationCosts) {
                    AiQuotaLine(text = usage.operationCostsLine(highlightOperation))
                }
            }
        }
    }
}

@Composable
private fun AiQuotaLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = neubrutalMutedOnSurface(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun AiUsage.compactQuotaText(): String {
    val percentage = if (pointLimit > 0) {
        ((pointsRemaining.toFloat() / pointLimit.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        0
    }
    val resetText = resetsAt?.let { " • Resets at ${formatQuotaResetTime(it)}" }.orEmpty()
    return "$percentage% usages left$resetText"
}

private fun AiUsage.operationCostsLine(highlight: AiQuotaOperation?): String {
    val items = listOf(
        labelFor(AiQuotaOperation.GENERATE, operationCosts.generate, highlight),
        labelFor(AiQuotaOperation.GRID_SPLIT, operationCosts.gridSplit, highlight),
        labelFor(AiQuotaOperation.BACKGROUND_REMOVE, operationCosts.backgroundRemove, highlight),
        labelFor(AiQuotaOperation.VIDEO_STICKER_PACK, operationCosts.videoStickerPack, highlight),
        labelFor(AiQuotaOperation.IMPROVE, operationCosts.improve, highlight),
        labelFor(AiQuotaOperation.PACK_IMPORT, operationCosts.packImport, highlight),
    ).filter { it.second > 0 }
    return items.joinToString(" · ") { (label, cost, isHighlight) ->
        if (isHighlight) "$label (−$cost)*" else "$label (−$cost)"
    }
}

private fun labelFor(operation: AiQuotaOperation, cost: Int, highlight: AiQuotaOperation?): Triple<String, Int, Boolean> {
    val label = when (operation) {
        AiQuotaOperation.GENERATE -> "Generate"
        AiQuotaOperation.GRID_SPLIT -> "Grid"
        AiQuotaOperation.BACKGROUND_REMOVE -> "BG"
        AiQuotaOperation.VIDEO_STICKER_PACK -> "Video"
        AiQuotaOperation.IMPROVE -> "Improve"
        AiQuotaOperation.PACK_IMPORT -> "Import"
    }
    return Triple(label, cost, operation == highlight)
}

private fun formatQuotaResetTime(value: String): String {
    return runCatching {
        val local = Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault())
        val minute = local.minute.toString().padStart(2, '0')
        "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')} ${local.hour}:$minute"
    }.getOrDefault(value)
}
