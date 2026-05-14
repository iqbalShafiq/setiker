package presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.neubrutalOnSurface

@Composable
fun ScreenSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = neubrutalOnSurface()
    )
}

// MARK: - Previews
@Preview
@Composable
private fun ScreenSectionTitlePreview() {
    MaterialTheme {
        ScreenSectionTitle(text = "Recent Stickers")
    }
}
