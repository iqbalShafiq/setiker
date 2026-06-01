package presentation.sharepreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.components.AppIllustration
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.AppTopBar
import presentation.components.InteractionBlockedBox
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back_content_description
import setiker.composeapp.generated.resources.share_preview_hint
import setiker.composeapp.generated.resources.share_preview_importing
import setiker.composeapp.generated.resources.share_preview_pack_fallback
import setiker.composeapp.generated.resources.share_preview_pack_title
import setiker.composeapp.generated.resources.share_preview_sticker_count
import setiker.composeapp.generated.resources.share_preview_sticker_fallback
import setiker.composeapp.generated.resources.share_preview_sticker_title
import setiker.composeapp.generated.resources.share_preview_unavailable_title

@Composable
fun SharePreviewScreen(
    state: SharePreviewState,
    onIntent: (SharePreviewIntent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.kind == "sticker") {
                    stringResource(Res.string.share_preview_sticker_title)
                } else {
                    stringResource(Res.string.share_preview_pack_title)
                },
                onBackClick = { onIntent(SharePreviewIntent.NavigateBack) }
            )
        },
        bottomBar = {
            PackBottomBar(
                actionStatusText = if (state.isAccepting) stringResource(Res.string.share_preview_importing) else null,
                actions = {
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Default.Check,
                        contentDescription = "Accept",
                        onClick = { onIntent(SharePreviewIntent.Accept) },
                        enabled = !state.isLoading && !state.isAccepting,
                        isLoading = state.isAccepting
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                illustration = AppIllustration.LoadingState
            )
            state.error != null -> EmptyState(
                title = stringResource(Res.string.share_preview_unavailable_title),
                description = state.error,
                illustration = AppIllustration.ErrorState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            else -> {
                InteractionBlockedBox(
                    blocked = state.isAccepting,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    Text(
                        text = if (state.kind == "sticker") {
                            stringResource(Res.string.share_preview_sticker_title)
                        } else {
                            stringResource(Res.string.share_preview_pack_title)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = neubrutalOnSurface()
                    )
                    if (state.kind == "sticker") {
                        Text(
                            text = state.stickerPreview?.sticker?.name ?: stringResource(Res.string.share_preview_sticker_fallback),
                            style = MaterialTheme.typography.titleMedium,
                            color = neubrutalOnSurface()
                        )
                    } else {
                        Text(
                            text = state.packPreview?.stickerPack?.name ?: stringResource(Res.string.share_preview_pack_fallback),
                            style = MaterialTheme.typography.titleMedium,
                            color = neubrutalOnSurface()
                        )
                        Text(
                            text = stringResource(
                                Res.string.share_preview_sticker_count,
                                state.packPreview?.stickerPack?.stickers?.size ?: 0
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = neubrutalMutedOnSurface()
                        )
                    }
                    Text(
                        text = if (state.isAccepting) {
                            stringResource(Res.string.share_preview_importing)
                        } else {
                            stringResource(Res.string.share_preview_hint)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = neubrutalMutedOnSurface(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    }
                }
            }
        }
    }
}
