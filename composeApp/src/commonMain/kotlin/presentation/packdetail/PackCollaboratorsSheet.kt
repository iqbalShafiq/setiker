package presentation.packdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import data.remote.model.PackCollaborator
import data.remote.model.UserSearchResult
import org.jetbrains.compose.resources.stringResource
import presentation.components.ShareCollaboratorUi
import presentation.components.ShareWithUsersSheet
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.pack_collaborators

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
    ShareWithUsersSheet(
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchResults = searchResults,
        collaborators = collaborators.map { it.toShareCollaboratorUi() },
        isLoading = isLoading,
        invitePermission = invitePermission,
        onInvitePermissionChange = onInvitePermissionChange,
        onInvite = onInvite,
        onRemove = onRemove,
        onRefresh = onRefresh,
        modifier = modifier,
        title = stringResource(Res.string.pack_collaborators)
    )
}

private fun PackCollaborator.toShareCollaboratorUi(): ShareCollaboratorUi = ShareCollaboratorUi(
    userId = sharedWithId,
    displayLabel = sharedWith?.displayName
        ?: sharedWith?.username
        ?: sharedWithId,
    permission = permission
)
