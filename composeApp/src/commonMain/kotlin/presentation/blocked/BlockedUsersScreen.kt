package presentation.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.remote.model.BlockedUserItem
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveLocal
import presentation.common.resolveOrDefault
import presentation.components.AppDialog
import presentation.components.AppTopBar
import presentation.components.ContentLoadLayout
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.blocked_users_empty_desc
import setiker.composeapp.generated.resources.blocked_users_empty_title
import setiker.composeapp.generated.resources.blocked_users_load_failed
import setiker.composeapp.generated.resources.blocked_users_title
import setiker.composeapp.generated.resources.blocked_users_unblock
import setiker.composeapp.generated.resources.blocked_users_unblock_confirm_message
import setiker.composeapp.generated.resources.blocked_users_unblock_confirm_title
import setiker.composeapp.generated.resources.cancel

@Composable
fun BlockedUsersScreenRoot(
    onBack: () -> Unit,
    viewModel: BlockedUsersViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                BlockedUsersEffect.NavigateBack -> onBack()
                is BlockedUsersEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
            }
        }
    }

    BlockedUsersScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun BlockedUsersScreen(
    state: BlockedUsersState,
    onIntent: (BlockedUsersIntent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    if (state.pendingUnblockUserId != null) {
        AppDialog(
            title = stringResource(Res.string.blocked_users_unblock_confirm_title),
            message = stringResource(Res.string.blocked_users_unblock_confirm_message),
            confirmText = stringResource(Res.string.blocked_users_unblock),
            dismissText = stringResource(Res.string.cancel),
            isDanger = false,
            onConfirm = { onIntent(BlockedUsersIntent.ConfirmUnblock) },
            onDismiss = { onIntent(BlockedUsersIntent.DismissUnblockConfirm) }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.blocked_users_title),
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        ContentLoadLayout(
            isLoading = state.isLoading,
            loadFailed = state.loadFailed,
            isEmpty = state.users.isEmpty(),
            emptyTitle = stringResource(Res.string.blocked_users_empty_title),
            emptyDescription = stringResource(Res.string.blocked_users_empty_desc),
            errorTitle = stringResource(Res.string.blocked_users_load_failed),
            onRetry = { onIntent(BlockedUsersIntent.Retry) },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.users, key = { it.id }) { user ->
                    BlockedUserRow(
                        user = user,
                        isUnblocking = state.unblockingUserId == user.id,
                        onUnblockClick = { onIntent(BlockedUsersIntent.ShowUnblockConfirm(user.id)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedUserRow(
    user: BlockedUserItem,
    isUnblocking: Boolean,
    onUnblockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = neubrutalBorderColor()
    val shadowColor = neubrutalShadowColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = shadowColor
            )
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(neubrutalCardSurface())
            .neubrutalBorderWithGloss(
                color = borderColor,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
        }

        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(NeubrutalCardRadius))
                .background(AccentCoral.copy(alpha = 0.12f))
                .neubrutalBorderWithGloss(
                    color = borderColor,
                    cornerRadius = NeubrutalCardRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .clickable(enabled = !isUnblocking, onClick = onUnblockClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isUnblocking) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp),
                    strokeWidth = 2.dp,
                    color = AccentCoral
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = null,
                    tint = AccentCoral,
                    modifier = Modifier.height(18.dp)
                )
            }
            Text(
                text = stringResource(Res.string.blocked_users_unblock),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccentCoral
            )
        }
    }
}
