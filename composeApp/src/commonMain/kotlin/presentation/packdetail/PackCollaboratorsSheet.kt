package presentation.packdetail

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
import data.remote.model.PackCollaborator
import data.remote.model.UserSearchResult
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalSelectableChip
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface

@Composable
fun PackCollaboratorsSheet(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<UserSearchResult>,
    collaborators: List<PackCollaborator>,
    isLoading: Boolean,
    invitePermission: String,
    onInvitePermissionChange: (String) -> Unit,
    onInvite: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Collaborators",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        AppTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = "Search users",
            placeholder = "Username"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeubrutalSelectableChip(
                label = "View",
                selected = invitePermission.equals("view", ignoreCase = true),
                onClick = { onInvitePermissionChange("view") }
            )
            NeubrutalSelectableChip(
                label = "Edit",
                selected = invitePermission.equals("edit", ignoreCase = true),
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
                        text = user.displayName ?: user.username ?: user.id,
                        style = MaterialTheme.typography.bodyLarge,
                        color = neubrutalOnSurface()
                    )
                    user.username?.let { username ->
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodySmall,
                            color = neubrutalMutedOnSurface()
                        )
                    }
                }
                AppSecondaryButton(
                    text = "Invite",
                    onClick = { onInvite(user.id) }
                )
            }
        }
        Text(
            text = "Shared with",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = neubrutalOnSurface()
        )
        if (collaborators.isEmpty()) {
            Text(
                text = "No collaborators yet",
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalMutedOnSurface()
            )
        } else {
            collaborators.forEach { collaborator ->
                val label = collaborator.sharedWith?.displayName
                    ?: collaborator.sharedWith?.username
                    ?: collaborator.sharedWithId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, color = neubrutalOnSurface())
                        Text(
                            text = collaborator.permission,
                            style = MaterialTheme.typography.bodySmall,
                            color = neubrutalMutedOnSurface()
                        )
                    }
                    AppSecondaryButton(
                        text = "Remove",
                        onClick = { onRemove(collaborator.sharedWithId) }
                    )
                }
            }
        }
        AppPrimaryButton(text = "Refresh", onClick = onRefresh)
    }
}
