package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import domain.model.AiQuotaOperation
import domain.model.AiUsage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalDialogRadius
import presentation.theme.NeubrutalLargeShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.ai_quota_costs_dialog_message
import setiker.composeapp.generated.resources.ai_quota_costs_dialog_title
import setiker.composeapp.generated.resources.ai_quota_costs_got_it
import setiker.composeapp.generated.resources.ai_quota_op_cost
import setiker.composeapp.generated.resources.ai_quota_op_generate
import setiker.composeapp.generated.resources.ai_quota_op_grid
import setiker.composeapp.generated.resources.ai_quota_op_import
import setiker.composeapp.generated.resources.ai_quota_op_improve
import setiker.composeapp.generated.resources.ai_quota_op_remove_bg
import setiker.composeapp.generated.resources.ai_quota_op_video

@Composable
fun AiQuotaCostsDialog(
    usage: AiUsage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalDialogRadius)
    val costRows = aiQuotaCostRows(usage)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .neubrutalShadow(
                    offsetX = NeubrutalLargeShadowOffset,
                    offsetY = NeubrutalLargeShadowOffset,
                    cornerRadius = NeubrutalDialogRadius,
                    color = shadow
                )
                .clip(shape)
                .background(neubrutalCardSurface())
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = NeubrutalDialogRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .padding(28.dp)
        ) {
            Text(
                text = stringResource(Res.string.ai_quota_costs_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.ai_quota_costs_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalMutedOnSurface()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                costRows.forEach { (labelRes, cost) ->
                    AiQuotaCostRow(
                        label = stringResource(labelRes),
                        cost = cost
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.ai_quota_costs_got_it),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AiQuotaCostRow(
    label: String,
    cost: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(neubrutalCardSurface())
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = neubrutalOnSurface(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(Res.string.ai_quota_op_cost, cost),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = AccentCoral
        )
    }
}

private fun aiQuotaCostRows(usage: AiUsage): List<Pair<StringResource, Int>> {
    return listOf(
        AiQuotaOperation.GENERATE to usage.operationCosts.generate,
        AiQuotaOperation.GRID_SPLIT to usage.operationCosts.gridSplit,
        AiQuotaOperation.BACKGROUND_REMOVE to usage.operationCosts.backgroundRemove,
        AiQuotaOperation.VIDEO_STICKER_PACK to usage.operationCosts.videoStickerPack,
        AiQuotaOperation.IMPROVE to usage.operationCosts.improve,
        AiQuotaOperation.PACK_IMPORT to usage.operationCosts.packImport
    ).mapNotNull { (operation, cost) ->
        if (cost <= 0) return@mapNotNull null
        operation.toOperationLabelRes() to cost
    }
}

private fun AiQuotaOperation.toOperationLabelRes(): StringResource = when (this) {
    AiQuotaOperation.GENERATE -> Res.string.ai_quota_op_generate
    AiQuotaOperation.GRID_SPLIT -> Res.string.ai_quota_op_grid
    AiQuotaOperation.BACKGROUND_REMOVE -> Res.string.ai_quota_op_remove_bg
    AiQuotaOperation.VIDEO_STICKER_PACK -> Res.string.ai_quota_op_video
    AiQuotaOperation.IMPROVE -> Res.string.ai_quota_op_improve
    AiQuotaOperation.PACK_IMPORT -> Res.string.ai_quota_op_import
}
