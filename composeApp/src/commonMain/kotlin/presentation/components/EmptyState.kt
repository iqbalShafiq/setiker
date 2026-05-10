package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.theme.NeubrutalDark
import presentation.theme.PastelMint
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.create_pack_title
import setiker.composeapp.generated.resources.no_stickers_yet_desc
import setiker.composeapp.generated.resources.no_stickers_yet_title

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .neubrutalShadow(
                    offsetX = 4.dp,
                    offsetY = 4.dp,
                    cornerRadius = 20.dp,
                    color = neubrutalShadowColor()
                )
                .clip(RoundedCornerShape(20.dp))
                .background(PastelMint)
                .border(
                    width = 2.dp,
                    color = neubrutalBorderColor(),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // PastelMint stays bright in both themes; keep contrast with NeubrutalDark.
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = NeubrutalDark
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = neubrutalOnSurface(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = neubrutalMutedOnSurface(),
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(28.dp))
            action()
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun EmptyStatePreview() {
    MaterialTheme {
        EmptyState(
            title = stringResource(Res.string.no_stickers_yet_title),
            description = stringResource(Res.string.no_stickers_yet_desc)
        )
    }
}

@Preview
@Composable
private fun EmptyStateWithActionPreview() {
    MaterialTheme {
        EmptyState(
            title = stringResource(Res.string.no_stickers_yet_title),
            description = stringResource(Res.string.no_stickers_yet_desc),
            action = { AppPrimaryButton(text = stringResource(Res.string.create_pack_title), onClick = {}) }
        )
    }
}
