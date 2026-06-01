package presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.model.AiQuotaOperation
import domain.model.AiUsage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.ai_quota_action_cost
import setiker.composeapp.generated.resources.ai_quota_loading
import setiker.composeapp.generated.resources.ai_quota_points_summary
import setiker.composeapp.generated.resources.ai_quota_resets
import setiker.composeapp.generated.resources.ai_quota_unavailable
import setiker.composeapp.generated.resources.ai_quota_usage_breakdown
import setiker.composeapp.generated.resources.settings_ai_usage_cost_generate
import setiker.composeapp.generated.resources.settings_ai_usage_cost_grid
import setiker.composeapp.generated.resources.settings_ai_usage_cost_improve
import setiker.composeapp.generated.resources.settings_ai_usage_cost_remove_bg
import setiker.composeapp.generated.resources.settings_ai_usage_cost_video

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
                Text(stringResource(Res.string.ai_quota_loading))
            }
            hasError -> {
                if (showIllustration) {
                    AppIllustrationImage(
                        illustration = AppIllustration.ErrorState,
                        modifier = Modifier.height(140.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(stringResource(Res.string.ai_quota_unavailable))
            }
            usage != null -> {
                Text(
                    text = stringResource(
                        Res.string.ai_quota_points_summary,
                        usage.pointsRemaining,
                        usage.pointLimit
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                usage.resetsAt?.let { resetsAt ->
                    Text(
                        text = stringResource(Res.string.ai_quota_resets, formatQuotaResetTime(resetsAt)),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.ai_quota_usage_breakdown,
                        usage.pointsUsed,
                        usage.pointsOutstanding
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                highlightOperation?.let { operation ->
                    val cost = usage.costFor(operation)
                    if (cost > 0) {
                        Text(
                            text = stringResource(Res.string.ai_quota_action_cost, cost),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                if (showOperationCosts) {
                    val costs = usage.operationCosts
                    Text(
                        text = stringResource(Res.string.settings_ai_usage_cost_generate, costs.generate),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = stringResource(Res.string.settings_ai_usage_cost_grid, costs.gridSplit),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(
                            Res.string.settings_ai_usage_cost_remove_bg,
                            costs.backgroundRemove
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(
                            Res.string.settings_ai_usage_cost_video,
                            costs.videoStickerPack
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(Res.string.settings_ai_usage_cost_improve, costs.improve),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun formatQuotaResetTime(value: String): String {
    return runCatching {
        val local = Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault())
        val minute = local.minute.toString().padStart(2, '0')
        "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')} ${local.hour}:$minute"
    }.getOrDefault(value)
}
