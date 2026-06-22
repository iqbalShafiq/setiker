package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.theme.NeubrutalCardRadius
import presentation.theme.PastelYellow
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.offline_banner_message

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier
) {
    AppInlineBanner(
        text = stringResource(Res.string.offline_banner_message),
        modifier = modifier,
        containerColor = PastelYellow
    )
}

@Composable
fun AppInlineBanner(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = PastelYellow
) {
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = neubrutalOnSurface(),
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                cornerRadius = NeubrutalCardRadius,
                color = neubrutalShadowColor()
            )
            .clip(shape)
            .background(containerColor)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
