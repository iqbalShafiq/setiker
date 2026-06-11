package presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveLocal
import presentation.components.AiQuotaSummary
import presentation.components.AppDialog
import presentation.components.AppIllustration
import presentation.components.AppIllustrationImage
import presentation.components.AppPasswordTextField
import presentation.components.AppTopBar
import presentation.components.KeyboardAwareLazyColumn
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back_content_description
import setiker.composeapp.generated.resources.settings_ai_usage_section
import setiker.composeapp.generated.resources.settings_change_password
import setiker.composeapp.generated.resources.settings_change_password_title
import setiker.composeapp.generated.resources.settings_confirm_password
import setiker.composeapp.generated.resources.settings_current_password
import setiker.composeapp.generated.resources.settings_delete_account
import setiker.composeapp.generated.resources.settings_delete_confirm_action
import setiker.composeapp.generated.resources.settings_delete_confirm_message
import setiker.composeapp.generated.resources.settings_delete_confirm_title
import setiker.composeapp.generated.resources.settings_new_password
import setiker.composeapp.generated.resources.settings_save_password_confirm_message
import setiker.composeapp.generated.resources.settings_show_onboarding
import setiker.composeapp.generated.resources.settings_title
import setiker.composeapp.generated.resources.settings_username
import util.rememberUrlLauncher

@Composable
fun SettingsScreenRoot(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    onShowOnboarding: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val openUrl = rememberUrlLauncher()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> onBack()
                SettingsEffect.NavigateToOnboarding -> onShowOnboarding()
                is SettingsEffect.OpenUrl -> openUrl(effect.url)
                SettingsEffect.AccountDeleted -> onAccountDeleted()
                is SettingsEffect.ShowMessage -> Unit
            }
        }
    }
    SettingsScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit
) {
    val canSavePassword = state.currentPassword.isNotBlank() &&
        state.newPassword.isNotBlank() &&
        state.confirmPassword.isNotBlank() &&
        !state.isChangingPassword

    if (state.showSavePasswordConfirm) {
        AppDialog(
            title = stringResource(Res.string.settings_change_password_title),
            message = stringResource(Res.string.settings_save_password_confirm_message),
            confirmText = stringResource(Res.string.settings_change_password),
            onConfirm = { onIntent(SettingsIntent.SubmitChangePassword) },
            onDismiss = { onIntent(SettingsIntent.DismissSavePasswordConfirm) },
            isDanger = false
        )
    }

    if (state.showDeleteConfirm) {
        AppDialog(
            title = stringResource(Res.string.settings_delete_confirm_title),
            message = stringResource(Res.string.settings_delete_confirm_message),
            confirmText = stringResource(Res.string.settings_delete_confirm_action),
            onConfirm = { onIntent(SettingsIntent.ConfirmDeleteAccount) },
            onDismiss = { onIntent(SettingsIntent.DismissDeleteConfirm) }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(Res.string.settings_title))
        },
        bottomBar = {
            PackBottomBar(
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_content_description),
                        onClick = { onIntent(SettingsIntent.NavigateBack) },
                        enabled = !state.isChangingPassword && !state.isDeletingAccount
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.School,
                        contentDescription = stringResource(Res.string.settings_show_onboarding),
                        onClick = { onIntent(SettingsIntent.ShowOnboardingAgain) },
                        enabled = !state.isChangingPassword && !state.isDeletingAccount
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.settings_delete_account),
                        onClick = { onIntent(SettingsIntent.ShowDeleteConfirm) },
                        enabled = !state.isChangingPassword && !state.isDeletingAccount
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Default.Save,
                        contentDescription = stringResource(Res.string.settings_change_password),
                        onClick = { onIntent(SettingsIntent.ShowSavePasswordConfirm) },
                        enabled = canSavePassword,
                        isLoading = state.isChangingPassword
                    )
                }
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        KeyboardAwareLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppIllustrationImage(
                    illustration = AppIllustration.AuthCloud,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .padding(top = 12.dp)
                )
            }
            item {
                Text(
                    text = stringResource(Res.string.settings_username),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = neubrutalOnSurface()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.username.ifBlank { "-" },
                    style = MaterialTheme.typography.bodySmall,
                    color = neubrutalMutedOnSurface()
                )
            }
            item {
                Text(text = stringResource(Res.string.settings_ai_usage_section))
                Spacer(modifier = Modifier.height(8.dp))
                AiQuotaSummary(
                    usage = state.aiUsage,
                    isLoading = state.isLoadingUsage,
                    hasError = state.usageError,
                    showIllustration = true
                )
            }
            item {
                AppPasswordTextField(
                    value = state.currentPassword,
                    onValueChange = { onIntent(SettingsIntent.UpdateCurrentPassword(it)) },
                    label = stringResource(Res.string.settings_current_password),
                    placeholder = stringResource(Res.string.settings_current_password),
                    imeAction = ImeAction.Next
                )
            }
            item {
                AppPasswordTextField(
                    value = state.newPassword,
                    onValueChange = { onIntent(SettingsIntent.UpdateNewPassword(it)) },
                    label = stringResource(Res.string.settings_new_password),
                    placeholder = stringResource(Res.string.settings_new_password),
                    imeAction = ImeAction.Next
                )
            }
            item {
                AppPasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = { onIntent(SettingsIntent.UpdateConfirmPassword(it)) },
                    label = stringResource(Res.string.settings_confirm_password),
                    placeholder = stringResource(Res.string.settings_confirm_password),
                    isError = state.changePasswordError != null,
                    supportingText = state.changePasswordError?.let { error ->
                        { Text(text = error.resolveLocal()) }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
