package presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppModalBottomSheet
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.keyboardAwareInsets
import presentation.components.rememberAppSheetState
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
    val sheetState = rememberAppSheetState()
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .keyboardAwareInsets()
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
            AppPrimaryButton(
                text = stringResource(Res.string.quota_exceeded_get_more),
                onClick = {
                    onDismiss()
                    onGetMore()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            AppSecondaryButton(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
