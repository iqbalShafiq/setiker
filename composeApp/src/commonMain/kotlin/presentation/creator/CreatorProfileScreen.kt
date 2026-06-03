package presentation.creator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.components.AppPrimaryButton
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarIconButton
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground

@Composable
fun CreatorProfileScreen(
    state: CreatorProfileState,
    onIntent: (CreatorProfileIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = state.profile?.displayName ?: state.profile?.username ?: "Creator",
                onBackClick = null
            )
        },
        bottomBar = {
            PackBottomBar(
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { onIntent(CreatorProfileIntent.NavigateBack) }
                    )
                },
                floatingActionButton = null
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.fillMaxSize().padding(innerPadding))
            state.loadFailed || state.profile == null -> EmptyState(
                title = "Creator not found",
                description = "This profile is unavailable.",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
            else -> {
                val profile = state.profile
                LazyColumn(
                    modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item("header") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "@${profile.username}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = neubrutalOnSurface()
                            )
                            Text(
                                text = "${profile.followerCount} followers · ${profile.publicPackCount} packs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = neubrutalMutedOnSurface()
                            )
                            AppPrimaryButton(
                                text = if (profile.isFollowing) "Following" else "Follow",
                                onClick = { onIntent(CreatorProfileIntent.ToggleFollow) },
                                enabled = !state.isFollowLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    items(state.packs, key = { it.id }) { pack ->
                        Text(
                            text = pack.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(CreatorProfileIntent.OpenPack(pack.id)) }
                                .padding(vertical = 8.dp),
                            color = neubrutalOnSurface()
                        )
                    }
                }
            }
        }
    }
}
