package presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.User
import domain.model.UserRole
import presentation.components.AppPrimaryButton
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProfileScreenRoot(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    ProfileScreen(
        state = state,
        onLogout = {
            viewModel.logout()
            onLogout()
        },
        modifier = modifier
    )
}

@Composable
fun ProfileScreen(
    state: ProfileState,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            state.user?.let { user ->
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = neubrutalOnSurface()
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (user.name != null) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = neubrutalOnSurface()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AppPrimaryButton(
                    text = "Logout",
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                )
            } ?: run {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "Not logged in",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileState(
                user = User(
                    id = "1",
                    username = "johndoe",
                    email = "john@example.com",
                    name = "John Doe",
                    role = UserRole(id = "1", name = "user"),
                    isActive = true,
                    createdAt = 0L
                ),
                isLoading = false
            ),
            onLogout = {}
        )
    }
}

@Preview
@Composable
private fun ProfileScreenLoadingPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileState(isLoading = true),
            onLogout = {}
        )
    }
}

@Preview
@Composable
private fun ProfileScreenEmptyPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileState(user = null, isLoading = false),
            onLogout = {}
        )
    }
}
