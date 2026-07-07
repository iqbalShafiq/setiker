package presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.components.zeroBottomSheetWindowInsets
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.quota_exceeded_get_more
import setiker.composeapp.generated.resources.quota_exceeded_sheet_message
import setiker.composeapp.generated.resources.quota_exceeded_sheet_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotaExceededBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onGetMore: () -> Unit
) {
    if (!visible) return
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
                text = stringResource(Res.string.quota_exceeded_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.quota_exceeded_sheet_message),
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalSubtleOnSurface()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onDismiss()
                    onGetMore()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.quota_exceeded_get_more))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    }
}
