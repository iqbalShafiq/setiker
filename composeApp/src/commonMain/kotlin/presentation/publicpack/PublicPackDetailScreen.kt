package presentation.publicpack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.AppTopBar
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back_content_description
import setiker.composeapp.generated.resources.explore_pack_not_found_desc
import setiker.composeapp.generated.resources.explore_pack_not_found_title
import setiker.composeapp.generated.resources.processing
import setiker.composeapp.generated.resources.public_pack_by
import setiker.composeapp.generated.resources.social_counts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicPackDetailScreen(
    state: PublicPackDetailState,
    onIntent: (PublicPackDetailIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = state.pack?.name ?: "",
                onBackClick = { onIntent(PublicPackDetailIntent.NavigateBack) }
            )
        },
        bottomBar = {
            PackBottomBar(
                actionStatusText = if (state.isActionLoading) stringResource(Res.string.processing) else null,
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.Default.Favorite,
                        contentDescription = if (state.isLiked) "Unlike" else "Like",
                        onClick = { onIntent(PublicPackDetailIntent.ToggleLike) },
                        enabled = !state.isActionLoading
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.Save,
                        contentDescription = if (state.isSaved) "Unsave" else "Save",
                        onClick = { onIntent(PublicPackDetailIntent.ToggleSave) },
                        enabled = !state.isActionLoading
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.PersonAdd,
                        contentDescription = if (state.isFollowingCreator) "Unfollow" else "Follow",
                        onClick = { onIntent(PublicPackDetailIntent.ToggleFollowCreator) },
                        enabled = !state.isActionLoading
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Default.Download,
                        contentDescription = "Import",
                        onClick = { onIntent(PublicPackDetailIntent.ImportPack) },
                        enabled = !state.isActionLoading && state.pack != null,
                        isLoading = state.isActionLoading
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
                    .padding(innerPadding)
            )
            state.pack == null -> EmptyState(
                title = stringResource(Res.string.explore_pack_not_found_title),
                description = state.error ?: stringResource(Res.string.explore_pack_not_found_desc),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            else -> {
                val pack = state.pack
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = neubrutalOnSurface()
                    )
                    Text(
                        text = stringResource(
                            Res.string.public_pack_by,
                            pack.owner?.displayName ?: pack.owner?.username ?: "Creator"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = neubrutalMutedOnSurface()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.social_counts, pack.likeCount, pack.saveCount, pack.downloadCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pack.stickers, key = { it.id ?: it.stickerId }) { relation ->
                            val sticker = relation.sticker
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neubrutalShadow(
                                        offsetX = NeubrutalSmallShadowOffset,
                                        offsetY = NeubrutalSmallShadowOffset,
                                        cornerRadius = NeubrutalCardRadius,
                                        color = neubrutalShadowColor()
                                    )
                                    .clip(RoundedCornerShape(NeubrutalCardRadius))
                                    .background(neubrutalCardSurface())
                                    .border(
                                        width = NeubrutalBorderWidth,
                                        color = neubrutalBorderColor(),
                                        shape = RoundedCornerShape(NeubrutalCardRadius)
                                    )
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = sticker?.url,
                                    contentDescription = sticker?.name ?: "Sticker",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(neubrutalScreenBackground()),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
