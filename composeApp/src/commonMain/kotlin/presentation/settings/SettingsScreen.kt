package presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.components.AiQuotaSummary
import presentation.components.AppDialog
import presentation.components.AppIllustration
import presentation.components.AppIllustrationImage
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.common.resolveLocal
import presentation.components.ProfileMenuItem
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.settings_account_section
import setiker.composeapp.generated.resources.settings_ai_usage_error
import setiker.composeapp.generated.resources.settings_ai_usage_loading
import setiker.composeapp.generated.resources.settings_ai_usage_resets
import setiker.composeapp.generated.resources.settings_ai_usage_section
import setiker.composeapp.generated.resources.settings_change_password
import setiker.composeapp.generated.resources.settings_change_password_title
import setiker.composeapp.generated.resources.settings_confirm_password
import setiker.composeapp.generated.resources.settings_current_password
import setiker.composeapp.generated.resources.settings_delete_account
import setiker.composeapp.generated.resources.settings_new_password
import setiker.composeapp.generated.resources.settings_delete_confirm_action
import setiker.composeapp.generated.resources.settings_delete_confirm_message
import setiker.composeapp.generated.resources.settings_delete_confirm_title
import setiker.composeapp.generated.resources.settings_legal_section
import setiker.composeapp.generated.resources.settings_privacy
import setiker.composeapp.generated.resources.settings_retention
import setiker.composeapp.generated.resources.settings_show_onboarding
import setiker.composeapp.generated.resources.settings_terms
import setiker.composeapp.generated.resources.settings_title
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
    if (state.showChangePassword) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissChangePassword) },
            title = { Text(stringResource(Res.string.settings_change_password_title)) },
            text = {
                Column {
                    AppTextField(
                        value = state.currentPassword,
                        onValueChange = { onIntent(SettingsIntent.UpdateCurrentPassword(it)) },
                        label = stringResource(Res.string.settings_current_password)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = state.newPassword,
                        onValueChange = { onIntent(SettingsIntent.UpdateNewPassword(it)) },
                        label = stringResource(Res.string.settings_new_password)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = state.confirmPassword,
                        onValueChange = { onIntent(SettingsIntent.UpdateConfirmPassword(it)) },
                        label = stringResource(Res.string.settings_confirm_password)
                    )
                    state.changePasswordError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error.resolveLocal())
                    }
                }
            },
            confirmButton = {
                AppPrimaryButton(
                    text = stringResource(Res.string.settings_change_password),
                    enabled = !state.isChangingPassword,
                    onClick = { onIntent(SettingsIntent.SubmitChangePassword) }
                )
            },
            dismissButton = {
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { onIntent(SettingsIntent.DismissChangePassword) }
                )
            }
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
            AppTopBar(
                title = stringResource(Res.string.settings_title),
                onBackClick = { onIntent(SettingsIntent.NavigateBack) }
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                AppIllustrationImage(
                    illustration = AppIllustration.AuthCloud,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(top = 12.dp)
                )
            }
            item {
                Text(
                    text = stringResource(Res.string.settings_ai_usage_section),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                AiQuotaSummary(
                    usage = state.aiUsage,
                    isLoading = state.isLoadingUsage,
                    hasError = state.usageError,
                    showOperationCosts = true,
                    showIllustration = true
                )
            }
            item {
                Text(
                    text = stringResource(Res.string.settings_legal_section),
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                ProfileMenuItem(
                    icon = Icons.Default.Description,
                    label = stringResource(Res.string.settings_privacy),
                    onClick = { onIntent(SettingsIntent.OpenPrivacy) },
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    label = stringResource(Res.string.settings_terms),
                    onClick = { onIntent(SettingsIntent.OpenTerms) },
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileMenuItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(Res.string.settings_retention),
                    onClick = { onIntent(SettingsIntent.OpenRetention) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    text = stringResource(Res.string.settings_account_section),
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    label = stringResource(Res.string.settings_change_password),
                    onClick = { onIntent(SettingsIntent.ShowChangePassword) },
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileMenuItem(
                    icon = Icons.Default.School,
                    label = stringResource(Res.string.settings_show_onboarding),
                    onClick = { onIntent(SettingsIntent.ShowOnboardingAgain) },
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileMenuItem(
                    icon = Icons.Default.Delete,
                    label = stringResource(Res.string.settings_delete_account),
                    onClick = { onIntent(SettingsIntent.ShowDeleteConfirm) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.isDeletingAccount) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
