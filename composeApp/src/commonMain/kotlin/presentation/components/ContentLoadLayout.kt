package presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import presentation.common.ContentStateAnimations
import presentation.common.ListLoadPhase
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.retry

/**
 * Shared loading / error / empty / content layout for list-style screens
 * (blocked users, purchase history, paywall catalog, etc.).
 */
@Composable
fun ContentLoadLayout(
    isLoading: Boolean,
    loadFailed: Boolean,
    isEmpty: Boolean,
    emptyTitle: String,
    emptyDescription: String,
    errorTitle: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyIllustration: AppIllustration = AppIllustration.SearchEmpty,
    errorIllustration: AppIllustration = AppIllustration.ErrorState,
    hasContent: Boolean = !isEmpty,
    content: @Composable () -> Unit
) {
    val phase = when {
        isLoading && !hasContent -> ListLoadPhase.Loading
        loadFailed && !hasContent -> ListLoadPhase.Error
        isEmpty -> ListLoadPhase.Empty
        else -> ListLoadPhase.Content
    }

    AnimatedContent(
        targetState = phase,
        transitionSpec = { with(ContentStateAnimations) { listLoad() } },
        label = "list_load_phase",
        modifier = modifier
    ) { current ->
        when (current) {
            ListLoadPhase.Loading -> {
                LoadingIndicator(modifier = Modifier.fillMaxSize())
            }
            ListLoadPhase.Error -> {
                EmptyState(
                    title = errorTitle,
                    description = "",
                    illustration = errorIllustration,
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        AppPrimaryButton(
                            text = stringResource(Res.string.retry),
                            onClick = onRetry
                        )
                    }
                )
            }
            ListLoadPhase.Empty -> {
                EmptyState(
                    title = emptyTitle,
                    description = emptyDescription,
                    illustration = emptyIllustration,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ListLoadPhase.Content -> content()
        }
    }
}
