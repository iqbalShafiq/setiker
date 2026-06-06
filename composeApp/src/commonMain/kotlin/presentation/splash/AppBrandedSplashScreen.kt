package presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppLogoMark
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.splash_presented_by

/**
 * Branded splash content aligned with the Android Splash Screen API theme:
 * centered logo mark and attribution at the bottom of the screen.
 */
@Composable
fun AppBrandedSplashScreen(
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSystemInDarkTheme()) {
        SplashBranding.backgroundDark
    } else {
        SplashBranding.backgroundLight
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AppLogoMark(
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.splash_presented_by),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = neubrutalSubtleOnSurface(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
