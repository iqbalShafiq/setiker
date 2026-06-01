package presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.theme.PastelYellow
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.offline_banner_message

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PastelYellow,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, neubrutalBorderColor())
    ) {
        Text(
            text = stringResource(Res.string.offline_banner_message),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
