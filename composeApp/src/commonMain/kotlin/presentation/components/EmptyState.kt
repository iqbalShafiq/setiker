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
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.PastelMint
import presentation.theme.neubrutalShadow

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
                    color = NeubrutalBlack
                )
                .clip(RoundedCornerShape(20.dp))
                .background(PastelMint)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = NeubrutalBlack
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = NeubrutalBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = NeubrutalGray,
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
            title = "No Stickers Yet",
            description = "Create your first sticker pack to get started"
        )
    }
}

@Preview
@Composable
private fun EmptyStateWithActionPreview() {
    MaterialTheme {
        EmptyState(
            title = "No Stickers Yet",
            description = "Create your first sticker pack to get started",
            action = { AppPrimaryButton(text = "Create Pack", onClick = {}) }
        )
    }
}
