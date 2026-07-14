package presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.ContentStateAnimations
import presentation.common.resolveLocal
import presentation.common.resolveOrDefault
import presentation.components.AppTextField
import presentation.components.LoadingIndicator
import presentation.theme.neubrutalMutedOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.edit_profile_display_name
import setiker.composeapp.generated.resources.edit_profile_email_readonly
import setiker.composeapp.generated.resources.edit_profile_save
import setiker.composeapp.generated.resources.edit_profile_title
import setiker.composeapp.generated.resources.edit_profile_username
import setiker.composeapp.generated.resources.error_username_invalid
import setiker.composeapp.generated.resources.processing

@Composable
fun EditProfileScreenRoot(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EditProfileEffect.NavigateBack -> onBack()
                is EditProfileEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
            }
        }
    }

    EditProfileScreen(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun EditProfileScreen(
    state: EditProfileState,
    onIntent: (EditProfileIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state.isLoading,
        transitionSpec = { with(ContentStateAnimations) { formReveal() } },
        label = "edit_profile_form_reveal",
        modifier = modifier.fillMaxSize()
    ) { loading ->
        if (loading) {
            LoadingIndicator(modifier = Modifier.fillMaxSize())
        } else {
            AuthFormScaffold(
                title = stringResource(Res.string.edit_profile_title),
                isLoading = state.isSaving,
                loadingStatusText = stringResource(Res.string.processing),
                onPrimaryAction = { onIntent(EditProfileIntent.Save) },
                primaryFabIcon = Icons.Default.Check,
                primaryFabContentDescription = stringResource(Res.string.edit_profile_save),
                onSecondaryAction = { onIntent(EditProfileIntent.NavigateBack) },
                secondaryActionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                secondaryActionContentDescription = stringResource(Res.string.back),
                modifier = Modifier.fillMaxSize()
            ) {
                AppTextField(
                    value = state.email,
                    onValueChange = {},
                    label = stringResource(Res.string.edit_profile_email_readonly),
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = state.displayName,
                    onValueChange = { onIntent(EditProfileIntent.UpdateDisplayName(it)) },
                    label = stringResource(Res.string.edit_profile_display_name),
                    enabled = !state.isSaving,
                    isError = state.displayNameError != null,
                    supportingText = state.displayNameError?.let { err ->
                        { Text(err.resolveLocal(), color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = state.username,
                    onValueChange = { onIntent(EditProfileIntent.UpdateUsername(it)) },
                    label = stringResource(Res.string.edit_profile_username),
                    enabled = !state.isSaving,
                    isError = state.usernameError != null,
                    supportingText = state.usernameError?.let { err ->
                        { Text(err.resolveLocal(), color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(
                    visible = state.error != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.error?.resolveLocal().orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.error_username_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
            }
        }
    }
}
