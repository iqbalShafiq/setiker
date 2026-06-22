package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Lightweight full-screen scrim with a bouncing dot indicator. Used when we
 * need a quick "please wait" without exposing real progress (e.g. tiny saves).
 *
 * For long-running work where we can report a real percentage, use
 * [ProgressDialog] instead so the user sees actual progress.
 */
@Composable
fun BlockingLoadingOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppIllustrationImage(
                illustration = AppIllustration.LoadingState,
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .height(180.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LoadingIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// MARK: - Previews
@Preview
@Composable
private fun BlockingLoadingOverlayPreview() {
    MaterialTheme {
        BlockingLoadingOverlay(message = "Loading...")
    }
}
