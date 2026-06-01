package presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import presentation.theme.NeubrutalCardRadius
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.illustration_auth_cloud
import setiker.composeapp.generated.resources.illustration_auth_cloud_dark
import setiker.composeapp.generated.resources.illustration_error_state
import setiker.composeapp.generated.resources.illustration_error_state_dark
import setiker.composeapp.generated.resources.illustration_loading_state
import setiker.composeapp.generated.resources.illustration_loading_state_dark
import setiker.composeapp.generated.resources.illustration_search_empty
import setiker.composeapp.generated.resources.illustration_search_empty_dark
import setiker.composeapp.generated.resources.illustration_success_sync
import setiker.composeapp.generated.resources.illustration_success_sync_dark
import setiker.composeapp.generated.resources.illustration_video_tools
import setiker.composeapp.generated.resources.illustration_video_tools_dark
import setiker.composeapp.generated.resources.onboarding_ai_jobs
import setiker.composeapp.generated.resources.onboarding_ai_jobs_dark
import setiker.composeapp.generated.resources.onboarding_create_pack
import setiker.composeapp.generated.resources.onboarding_create_pack_dark
import setiker.composeapp.generated.resources.onboarding_edit_polish
import setiker.composeapp.generated.resources.onboarding_edit_polish_dark

data class AppIllustration(
    val light: DrawableResource,
    val dark: DrawableResource
) {
    companion object {
        val EmptyPack = AppIllustration(
            light = Res.drawable.onboarding_create_pack,
            dark = Res.drawable.onboarding_create_pack_dark
        )
        val EditorTools = AppIllustration(
            light = Res.drawable.onboarding_edit_polish,
            dark = Res.drawable.onboarding_edit_polish_dark
        )
        val AiJobs = AppIllustration(
            light = Res.drawable.onboarding_ai_jobs,
            dark = Res.drawable.onboarding_ai_jobs_dark
        )
        val AuthCloud = AppIllustration(
            light = Res.drawable.illustration_auth_cloud,
            dark = Res.drawable.illustration_auth_cloud_dark
        )
        val SearchEmpty = AppIllustration(
            light = Res.drawable.illustration_search_empty,
            dark = Res.drawable.illustration_search_empty_dark
        )
        val ErrorState = AppIllustration(
            light = Res.drawable.illustration_error_state,
            dark = Res.drawable.illustration_error_state_dark
        )
        val LoadingState = AppIllustration(
            light = Res.drawable.illustration_loading_state,
            dark = Res.drawable.illustration_loading_state_dark
        )
        val SuccessSync = AppIllustration(
            light = Res.drawable.illustration_success_sync,
            dark = Res.drawable.illustration_success_sync_dark
        )
        val VideoTools = AppIllustration(
            light = Res.drawable.illustration_video_tools,
            dark = Res.drawable.illustration_video_tools_dark
        )
    }
}

@Composable
fun AppIllustrationImage(
    illustration: AppIllustration,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val resource = if (isSystemInDarkTheme()) illustration.dark else illustration.light
    Image(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NeubrutalCardRadius))
    )
}
