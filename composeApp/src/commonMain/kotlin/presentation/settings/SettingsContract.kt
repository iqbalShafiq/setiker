package presentation.settings

import domain.model.AiUsage
import domain.model.LegalSummary
import presentation.common.UiText

data class SettingsState(
    val isLoadingLegal: Boolean = true,
    val legalSummary: LegalSummary? = null,
    val aiUsage: AiUsage? = null,
    val isLoadingUsage: Boolean = false,
    val usageError: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showChangePassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val isChangingPassword: Boolean = false,
    val changePasswordError: UiText? = null,
    val showSavePasswordConfirm: Boolean = false
)

sealed interface SettingsIntent {
    data object Load : SettingsIntent
    data object ShowDeleteConfirm : SettingsIntent
    data object DismissDeleteConfirm : SettingsIntent
    data object ConfirmDeleteAccount : SettingsIntent
    data object ShowOnboardingAgain : SettingsIntent
    data object NavigateBack : SettingsIntent
    data object OpenPrivacy : SettingsIntent
    data object OpenTerms : SettingsIntent
    data object OpenRetention : SettingsIntent
    data object ShowChangePassword : SettingsIntent
    data object DismissChangePassword : SettingsIntent
    data class UpdateCurrentPassword(val value: String) : SettingsIntent
    data class UpdateNewPassword(val value: String) : SettingsIntent
    data class UpdateConfirmPassword(val value: String) : SettingsIntent
    data object SubmitChangePassword : SettingsIntent
    data object ShowSavePasswordConfirm : SettingsIntent
    data object DismissSavePasswordConfirm : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToOnboarding : SettingsEffect
    data class OpenUrl(val url: String) : SettingsEffect
    data class ShowMessage(val message: UiText) : SettingsEffect
    data object AccountDeleted : SettingsEffect
}
