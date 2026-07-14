package presentation.auth

import presentation.common.UiText

data class EditProfileState(
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val displayNameError: UiText? = null,
    val usernameError: UiText? = null
)

sealed interface EditProfileIntent {
    data object Load : EditProfileIntent
    data class UpdateDisplayName(val value: String) : EditProfileIntent
    data class UpdateUsername(val value: String) : EditProfileIntent
    data object Save : EditProfileIntent
    data object NavigateBack : EditProfileIntent
}

sealed interface EditProfileEffect {
    data object NavigateBack : EditProfileEffect
    data class ShowMessage(val message: UiText) : EditProfileEffect
}
