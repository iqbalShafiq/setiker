package presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.remote.model.UserSearchResult
import org.jetbrains.compose.resources.stringResource
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.share_with_users_empty
import setiker.composeapp.generated.resources.share_with_users_invite
import setiker.composeapp.generated.resources.share_with_users_permission_edit
import setiker.composeapp.generated.resources.share_with_users_permission_view
import setiker.composeapp.generated.resources.share_with_users_refresh
import setiker.composeapp.generated.resources.share_with_users_remove
import setiker.composeapp.generated.resources.share_with_users_search_label
import setiker.composeapp.generated.resources.share_with_users_search_placeholder
import setiker.composeapp.generated.resources.share_with_users_shared_with
import setiker.composeapp.generated.resources.share_with_users_title

data class ShareCollaboratorUi(
    val userId: String,
    val displayLabel: String,
    val permission: String
)

@Composable
fun ShareWithUsersSheet(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<UserSearchResult>,
    collaborators: List<ShareCollaboratorUi>,
    isLoading: Boolean,
    invitePermission: String,
    onInvitePermissionChange: (String) -> Unit,
    onInvite: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.share_with_users_title)
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        AppTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = stringResource(Res.string.share_with_users_search_label),
            placeholder = stringResource(Res.string.share_with_users_search_placeholder)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeubrutalSelectableChip(
                label = stringResource(Res.string.share_with_users_permission_view),
                selected = invitePermission.equals("view", ignoreCase = true),
                onClick = { onInvitePermissionChange("view") }
            )
            NeubrutalSelectableChip(
                label = stringResource(Res.string.share_with_users_permission_edit),
                selected = invitePermission.equals("edit", ignoreCase = true) ||
                    invitePermission.equals("full", ignoreCase = true),
                onClick = { onInvitePermissionChange("edit") }
            )
        }
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
        searchResults.forEach { user ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                        style = MaterialTheme.typography.bodyLarge,
                        color = neubrutalOnSurface()
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface()
                    )
                }
                AppSecondaryButton(
                    text = stringResource(Res.string.share_with_users_invite),
                    onClick = { onInvite(user.id) }
                )
            }
        }
        Text(
            text = stringResource(Res.string.share_with_users_shared_with),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface()
        )
        if (collaborators.isEmpty()) {
            Text(
                text = stringResource(Res.string.share_with_users_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalMutedOnSurface()
            )
        } else {
            collaborators.forEach { collaborator ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = collaborator.displayLabel, color = neubrutalOnSurface())
                        Text(
                            text = collaborator.permission,
                            style = MaterialTheme.typography.bodySmall,
                            color = neubrutalMutedOnSurface()
                        )
                    }
                    AppSecondaryButton(
                        text = stringResource(Res.string.share_with_users_remove),
                        onClick = { onRemove(collaborator.userId) }
                    )
                }
            }
        }
        AppPrimaryButton(
            text = stringResource(Res.string.share_with_users_refresh),
            onClick = onRefresh
        )
    }
}
