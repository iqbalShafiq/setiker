package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.model.UpdateProfileRequest
import data.auth.model.toDomainModel
import data.remote.ApiException
import domain.error.AppErrorCode
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
import setiker.composeapp.generated.resources.edit_profile_success
import setiker.composeapp.generated.resources.error_edit_profile_failed
import setiker.composeapp.generated.resources.error_username_invalid
import setiker.composeapp.generated.resources.error_username_taken

class EditProfileViewModel(
    private val authManager: AuthManager,
    private val authApiService: AuthApiService
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _effect = Channel<EditProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: EditProfileIntent) {
        when (intent) {
            EditProfileIntent.Load -> load()
            is EditProfileIntent.UpdateDisplayName -> _state.update {
                it.copy(displayName = intent.value, displayNameError = null, error = null)
            }
            is EditProfileIntent.UpdateUsername -> _state.update {
                it.copy(username = intent.value, usernameError = null, error = null)
            }
            EditProfileIntent.Save -> save()
            EditProfileIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(EditProfileEffect.NavigateBack)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val cached = authManager.getUser()
            val token = authManager.getValidAccessToken()
            val user = if (token != null) {
                runCatching {
                    authApiService.getProfile(token).data?.toDomainModel()?.also { authManager.saveUser(it) }
                }.getOrNull() ?: cached
            } else {
                cached
            }
            if (user == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringRes(Res.string.error_edit_profile_failed)
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    email = user.email,
                    displayName = user.name.orEmpty(),
                    username = user.username
                )
            }
        }
    }

    private fun save() {
        val current = _state.value
        val username = current.username.trim()
        if (!USERNAME_REGEX.matches(username)) {
            _state.update {
                it.copy(usernameError = UiText.StringRes(Res.string.error_username_invalid))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, usernameError = null) }
            val token = authManager.getValidAccessToken()
            if (token.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = UiText.StringRes(Res.string.error_edit_profile_failed)
                    )
                }
                return@launch
            }
            runCatching {
                authApiService.updateProfile(
                    token = token,
                    request = UpdateProfileRequest(
                        displayName = current.displayName.trim().ifEmpty { null },
                        username = username
                    )
                )
            }.onSuccess { response ->
                val user = response.data?.toDomainModel()
                if (user != null) {
                    authManager.saveUser(user)
                }
                _state.update { it.copy(isSaving = false) }
                _effect.send(EditProfileEffect.ShowMessage(UiText.StringRes(Res.string.edit_profile_success)))
                _effect.send(EditProfileEffect.NavigateBack)
            }.onFailure { error ->
                val uiError = when ((error as? ApiException)?.code) {
                    AppErrorCode.AuthUsernameTaken -> error.toUiText(Res.string.error_username_taken)
                    else -> error.toUiText(Res.string.error_edit_profile_failed)
                }
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = uiError,
                        usernameError = if ((error as? ApiException)?.code == AppErrorCode.AuthUsernameTaken) {
                            uiError
                        } else {
                            it.usernameError
                        }
                    )
                }
            }
        }
    }

    companion object {
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,30}$")
    }
}
