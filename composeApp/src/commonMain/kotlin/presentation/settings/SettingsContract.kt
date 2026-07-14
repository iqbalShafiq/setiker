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
    val deleteConfirmPassword: String = "",
    val deleteConfirmPhrase: String = "",
    val deleteAccountError: UiText? = null,
    val showChangePassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val hasPassword: Boolean = true,
    val hasGoogle: Boolean = false,
    val googleAvailable: Boolean = false,
    val isChangingPassword: Boolean = false,
    val changePasswordError: UiText? = null,
    val showSavePasswordConfirm: Boolean = false,
    val showSetPassword: Boolean = false,
    val isLinkingGoogle: Boolean = false,
    val isSettingPassword: Boolean = false
)

sealed interface SettingsIntent {
    data object Load : SettingsIntent
    data object ShowDeleteConfirm : SettingsIntent
    data object DismissDeleteConfirm : SettingsIntent
    data object ConfirmDeleteAccount : SettingsIntent
    data class UpdateDeleteConfirmPassword(val value: String) : SettingsIntent
    data class UpdateDeleteConfirmPhrase(val value: String) : SettingsIntent
    data object ShowOnboardingAgain : SettingsIntent
    data object NavigateBack : SettingsIntent
    data object OpenPrivacy : SettingsIntent
    data object OpenTerms : SettingsIntent
    data object OpenRetention : SettingsIntent
    data object OpenPurchaseHistory : SettingsIntent
    data object ShowChangePassword : SettingsIntent
    data object DismissChangePassword : SettingsIntent
    data class UpdateCurrentPassword(val value: String) : SettingsIntent
    data class UpdateNewPassword(val value: String) : SettingsIntent
    data class UpdateConfirmPassword(val value: String) : SettingsIntent
    data object SubmitChangePassword : SettingsIntent
    data object ShowSavePasswordConfirm : SettingsIntent
    data object DismissSavePasswordConfirm : SettingsIntent
    data object LinkGoogle : SettingsIntent
    data object UnlinkGoogle : SettingsIntent
    data object ShowSetPassword : SettingsIntent
    data object DismissSetPassword : SettingsIntent
    data object SubmitSetPassword : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToOnboarding : SettingsEffect
    data object NavigateToPurchaseHistory : SettingsEffect
    data class OpenUrl(val url: String) : SettingsEffect
    data class ShowMessage(val message: UiText) : SettingsEffect
    data object AccountDeleted : SettingsEffect
}
