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
import presentation.components.AppPrimaryButton
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
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
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
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
