package presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.report_content_details_hint
import setiker.composeapp.generated.resources.report_content_message
import setiker.composeapp.generated.resources.report_content_submit
import setiker.composeapp.generated.resources.report_content_title
import setiker.composeapp.generated.resources.report_reason_child_safety
import setiker.composeapp.generated.resources.report_reason_deceptive
import setiker.composeapp.generated.resources.report_reason_hate
import setiker.composeapp.generated.resources.report_reason_illegal
import setiker.composeapp.generated.resources.report_reason_ip
import setiker.composeapp.generated.resources.report_reason_other
import setiker.composeapp.generated.resources.report_reason_sexual
import setiker.composeapp.generated.resources.report_reason_violence

data class ReportReasonOption(
    val apiValue: String,
    val label: String
)

@Composable
fun rememberReportReasonOptions(): List<ReportReasonOption> = listOf(
    ReportReasonOption("SEXUAL", stringResource(Res.string.report_reason_sexual)),
    ReportReasonOption("VIOLENCE", stringResource(Res.string.report_reason_violence)),
    ReportReasonOption("CHILD_SAFETY", stringResource(Res.string.report_reason_child_safety)),
    ReportReasonOption("HATE_HARASSMENT", stringResource(Res.string.report_reason_hate)),
    ReportReasonOption("DECEPTIVE", stringResource(Res.string.report_reason_deceptive)),
    ReportReasonOption("IP_INFRINGEMENT", stringResource(Res.string.report_reason_ip)),
    ReportReasonOption("ILLEGAL", stringResource(Res.string.report_reason_illegal)),
    ReportReasonOption("OTHER", stringResource(Res.string.report_reason_other))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportContentBottomSheet(
    visible: Boolean,
    selectedReason: String?,
    details: String,
    isSubmitting: Boolean,
    onReasonSelected: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val reasons = rememberReportReasonOptions()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { zeroBottomSheetWindowInsets() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(Res.string.report_content_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.report_content_message),
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalSubtleOnSurface()
            )
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reasons.forEach { reason ->
                    NeubrutalSelectableChip(
                        selected = selectedReason == reason.apiValue,
                        onClick = { onReasonSelected(reason.apiValue) },
                        label = reason.label
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AppTextField(
                value = details,
                onValueChange = onDetailsChange,
                label = stringResource(Res.string.report_content_details_hint),
                placeholder = stringResource(Res.string.report_content_details_hint),
                singleLine = false,
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(20.dp))
            AppPrimaryButton(
                text = stringResource(Res.string.report_content_submit),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !selectedReason.isNullOrBlank() && !isSubmitting
            )
            Spacer(modifier = Modifier.height(10.dp))
            AppSecondaryButton(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            )
        }
    }
}
