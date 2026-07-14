package presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.AuthSessionCoordinator
import data.auth.model.ChangePasswordRequest
import data.remote.ApiException
import data.preferences.UserPreferencesRepository
import data.remote.AiUsageApiRepository
import data.remote.LegalApiRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_auth_not_authenticated
import setiker.composeapp.generated.resources.error_cloud_delete_failed
import setiker.composeapp.generated.resources.settings_password_changed
import setiker.composeapp.generated.resources.settings_password_mismatch
import setiker.composeapp.generated.resources.error_auth_change_password_failed

class SettingsViewModel(
    private val legalApiRepository: LegalApiRepository,
    private val aiUsageApiRepository: AiUsageApiRepository,
    private val authManager: AuthManager,
    private val authApiService: AuthApiService,
    private val authSessionCoordinator: AuthSessionCoordinator,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(SettingsIntent.Load)
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> load()
            SettingsIntent.ShowDeleteConfirm -> _state.update {
                it.copy(
                    showDeleteConfirm = true,
                    deleteConfirmPassword = "",
                    deleteConfirmPhrase = "",
                    deleteAccountError = null
                )
            }
            SettingsIntent.DismissDeleteConfirm -> _state.update {
                it.copy(
                    showDeleteConfirm = false,
                    deleteConfirmPassword = "",
                    deleteConfirmPhrase = "",
                    deleteAccountError = null
                )
            }
            SettingsIntent.ConfirmDeleteAccount -> deleteAccount()
            is SettingsIntent.UpdateDeleteConfirmPassword -> _state.update { it.copy(deleteConfirmPassword = intent.value) }
            is SettingsIntent.UpdateDeleteConfirmPhrase -> _state.update { it.copy(deleteConfirmPhrase = intent.value) }
            SettingsIntent.ShowOnboardingAgain -> viewModelScope.launch {
                userPreferencesRepository.setOnboardingCompleted(false)
                _effect.send(SettingsEffect.NavigateToOnboarding)
            }
            SettingsIntent.NavigateBack -> viewModelScope.launch { _effect.send(SettingsEffect.NavigateBack) }
            SettingsIntent.OpenPrivacy -> openLegalUrl { it.privacyUrl }
            SettingsIntent.OpenTerms -> openLegalUrl { it.termsUrl }
            SettingsIntent.OpenRetention -> openLegalUrl { it.retentionUrl ?: it.privacyUrl }
            SettingsIntent.OpenPurchaseHistory -> viewModelScope.launch {
                _effect.send(SettingsEffect.NavigateToPurchaseHistory)
            }
            SettingsIntent.ShowChangePassword -> _state.update {
                it.copy(showChangePassword = true, changePasswordError = null)
            }
            SettingsIntent.DismissChangePassword -> _state.update {
                it.copy(
                    showChangePassword = false,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    changePasswordError = null
                )
            }
            is SettingsIntent.UpdateCurrentPassword -> _state.update { it.copy(currentPassword = intent.value) }
            is SettingsIntent.UpdateNewPassword -> _state.update { it.copy(newPassword = intent.value) }
            is SettingsIntent.UpdateConfirmPassword -> _state.update { it.copy(confirmPassword = intent.value) }
            SettingsIntent.SubmitChangePassword -> changePassword()
            SettingsIntent.ShowSavePasswordConfirm -> _state.update { it.copy(showSavePasswordConfirm = true) }
            SettingsIntent.DismissSavePasswordConfirm -> _state.update { it.copy(showSavePasswordConfirm = false) }
        }
    }

    private fun changePassword() {
        val current = _state.value
        if (current.newPassword != current.confirmPassword) {
            _state.update {
                it.copy(changePasswordError = UiText.StringRes(Res.string.settings_password_mismatch))
            }
            return
        }
        viewModelScope.launch {
            val token = authManager.getValidAccessToken() ?: authManager.getAccessToken()
            if (token.isNullOrBlank()) {
                _effect.send(SettingsEffect.ShowMessage(UiText.StringRes(Res.string.error_auth_not_authenticated)))
                return@launch
            }
            _state.update { it.copy(isChangingPassword = true, changePasswordError = null, showSavePasswordConfirm = false) }
            runCatching {
                authApiService.changePassword(
                    token,
                    ChangePasswordRequest(
                        currentPassword = current.currentPassword,
                        newPassword = current.newPassword
                    )
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        isChangingPassword = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        showSavePasswordConfirm = false
                    )
                }
                _effect.send(SettingsEffect.ShowMessage(UiText.StringRes(Res.string.settings_password_changed)))
            }.onFailure { error ->
                val message = when (error) {
                    is ApiException -> error.toUiText(Res.string.error_auth_change_password_failed)
                    else -> error.toUiText(Res.string.error_auth_change_password_failed)
                }
                _state.update { it.copy(isChangingPassword = false, changePasswordError = message) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val user = authManager.getUser()
            _state.update {
                it.copy(
                    username = user?.username.orEmpty(),
                    isLoadingLegal = true,
                    isLoadingUsage = true,
                    usageError = false
                )
            }
            runCatching { legalApiRepository.getSummary() }
                .onSuccess { summary ->
                    _state.update { it.copy(isLoadingLegal = false, legalSummary = summary) }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingLegal = false) }
                }
            if (authManager.isAuthenticated()) {
                runCatching { aiUsageApiRepository.getUsage() }
                    .onSuccess { usage ->
                        _state.update { it.copy(isLoadingUsage = false, aiUsage = usage, usageError = false) }
                    }
                    .onFailure {
                        _state.update { it.copy(isLoadingUsage = false, usageError = true) }
                    }
            } else {
                _state.update { it.copy(isLoadingUsage = false) }
            }
        }
    }

    private fun openLegalUrl(selector: (domain.model.LegalSummary) -> String) {
        val summary = _state.value.legalSummary ?: return
        viewModelScope.launch {
            _effect.send(SettingsEffect.OpenUrl(selector(summary)))
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            val current = _state.value
            val token = authManager.getValidAccessToken() ?: authManager.getAccessToken()
            if (token.isNullOrBlank()) {
                _effect.send(SettingsEffect.ShowMessage(UiText.StringRes(Res.string.error_auth_not_authenticated)))
                return@launch
            }
            _state.update { it.copy(isDeletingAccount = true, deleteAccountError = null) }
            runCatching {
                authApiService.deleteAccount(token, current.deleteConfirmPassword)
            }.onSuccess {
                authSessionCoordinator.endSession()
                _state.update {
                    it.copy(
                        isDeletingAccount = false,
                        showDeleteConfirm = false,
                        deleteConfirmPassword = "",
                        deleteConfirmPhrase = ""
                    )
                }
                _effect.send(SettingsEffect.AccountDeleted)
            }.onFailure { error ->
                val message = when (error) {
                    is ApiException -> error.toUiText(Res.string.error_cloud_delete_failed)
                    else -> error.toUiText(Res.string.error_cloud_delete_failed)
                }
                _state.update { it.copy(isDeletingAccount = false, deleteAccountError = message) }
            }
        }
    }
}
