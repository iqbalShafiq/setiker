package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AppleSignInGateway
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.AuthSessionCoordinator
import data.auth.GoogleSignInGateway
import data.auth.GoogleSignInMode
import data.auth.model.toDomainModel
import data.remote.ExploreApiRepository
import data.remote.LegalApiRepository
import domain.model.AiUsage
import domain.model.LegalSummary
import domain.model.User
import domain.repository.AiQuotaRepository
import domain.repository.StickerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_auth_change_password_failed
import setiker.composeapp.generated.resources.error_auth_not_authenticated
import setiker.composeapp.generated.resources.settings_apple_linked
import setiker.composeapp.generated.resources.settings_apple_unlinked
import setiker.composeapp.generated.resources.settings_google_linked
import setiker.composeapp.generated.resources.settings_google_unlinked
import setiker.composeapp.generated.resources.settings_password_mismatch
import setiker.composeapp.generated.resources.settings_password_set_success

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val stickersCount: Int = 0,
    val packsCount: Int = 0,
    val downloadsCount: Int = 0,
    val aiUsage: AiUsage? = null,
    val isLoadingAiUsage: Boolean = false,
    val aiUsageLoadFailed: Boolean = false,
    val legalSummary: LegalSummary? = null,
    val notificationUnreadCount: Int = 0,
    val googleAvailable: Boolean = false,
    val appleAvailable: Boolean = false,
    val isLinkingAuth: Boolean = false,
    val showSetPassword: Boolean = false,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val setPasswordError: UiText? = null,
    val bannerMessage: UiText? = null
)

class ProfileViewModel(
    private val authManager: AuthManager,
    private val authSessionCoordinator: AuthSessionCoordinator,
    private val stickerRepository: StickerRepository,
    private val aiQuotaRepository: AiQuotaRepository,
    private val legalApiRepository: LegalApiRepository,
    private val exploreApiRepository: ExploreApiRepository,
    private val authApiService: AuthApiService,
    private val googleSignInGateway: GoogleSignInGateway,
    private val appleSignInGateway: AppleSignInGateway
) : ViewModel() {

    private val _state = MutableStateFlow(
        ProfileState(
            googleAvailable = googleSignInGateway.isAvailable(),
            appleAvailable = appleSignInGateway.isAvailable()
        )
    )
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init { loadUser() }

    fun refresh() = loadUser()

    fun dismissBanner() {
        _state.update { it.copy(bannerMessage = null) }
    }

    fun showSetPassword() {
        _state.update {
            it.copy(showSetPassword = true, newPassword = "", confirmPassword = "", setPasswordError = null)
        }
    }

    fun dismissSetPassword() {
        _state.update {
            it.copy(showSetPassword = false, newPassword = "", confirmPassword = "", setPasswordError = null)
        }
    }

    fun updateNewPassword(value: String) {
        _state.update { it.copy(newPassword = value, setPasswordError = null) }
    }

    fun updateConfirmPassword(value: String) {
        _state.update { it.copy(confirmPassword = value, setPasswordError = null) }
    }

    fun submitSetPassword() {
        val current = _state.value
        if (current.newPassword != current.confirmPassword) {
            _state.update {
                it.copy(setPasswordError = UiText.StringRes(Res.string.settings_password_mismatch))
            }
            return
        }
        viewModelScope.launch {
            val token = authManager.getValidAccessToken() ?: return@launch
            _state.update { it.copy(isLinkingAuth = true, setPasswordError = null) }
            runCatching { authApiService.setPassword(token, current.newPassword) }
                .onSuccess {
                    val user = authManager.getUser()?.copy(
                        hasPassword = true,
                        authProviders = (authManager.getUser()?.authProviders.orEmpty() + "PASSWORD").distinct()
                    )
                    if (user != null) authManager.saveUser(user)
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            showSetPassword = false,
                            user = user,
                            bannerMessage = UiText.StringRes(Res.string.settings_password_set_success)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            setPasswordError = error.toUiText(Res.string.error_auth_change_password_failed)
                        )
                    }
                }
        }
    }

    fun linkGoogle() {
        if (!googleSignInGateway.isAvailable()) return
        viewModelScope.launch {
            val token = authManager.getValidAccessToken()
            if (token.isNullOrBlank()) {
                _state.update {
                    it.copy(bannerMessage = UiText.StringRes(Res.string.error_auth_not_authenticated))
                }
                return@launch
            }
            _state.update { it.copy(isLinkingAuth = true) }
            val idTokenResult = googleSignInGateway.signIn(GoogleSignInMode.Button)
            idTokenResult.onFailure { error ->
                _state.update {
                    it.copy(
                        isLinkingAuth = false,
                        bannerMessage = error.toUiText(Res.string.error_auth_change_password_failed)
                    )
                }
                return@launch
            }
            val idToken = idTokenResult.getOrNull()?.idToken ?: return@launch
            runCatching { authApiService.linkGoogle(token, idToken) }
                .onSuccess { response ->
                    val user = response.data?.toDomainModel()
                    if (user != null) authManager.saveUser(user)
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            user = user ?: it.user,
                            bannerMessage = UiText.StringRes(Res.string.settings_google_linked)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            bannerMessage = error.toUiText(Res.string.error_auth_change_password_failed)
                        )
                    }
                }
        }
    }

    fun unlinkGoogle() {
        viewModelScope.launch {
            val token = authManager.getValidAccessToken() ?: return@launch
            _state.update { it.copy(isLinkingAuth = true) }
            runCatching { authApiService.unlinkGoogle(token) }
                .onSuccess { response ->
                    val user = response.data?.toDomainModel()
                    if (user != null) authManager.saveUser(user)
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            user = user ?: it.user,
                            bannerMessage = UiText.StringRes(Res.string.settings_google_unlinked)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            bannerMessage = error.toUiText(Res.string.error_auth_change_password_failed)
                        )
                    }
                }
        }
    }

    fun linkApple() {
        if (!appleSignInGateway.isAvailable()) return
        viewModelScope.launch {
            val token = authManager.getValidAccessToken() ?: return@launch
            _state.update { it.copy(isLinkingAuth = true) }
            val idTokenResult = appleSignInGateway.signIn()
            idTokenResult.onFailure { error ->
                _state.update {
                    it.copy(
                        isLinkingAuth = false,
                        bannerMessage = error.toUiText(Res.string.error_auth_change_password_failed)
                    )
                }
                return@launch
            }
            val idToken = idTokenResult.getOrNull()?.idToken ?: return@launch
            runCatching { authApiService.linkApple(token, idToken) }
                .onSuccess { response ->
                    val user = response.data?.toDomainModel()
                    if (user != null) authManager.saveUser(user)
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            user = user ?: it.user,
                            bannerMessage = UiText.StringRes(Res.string.settings_apple_linked)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            bannerMessage = error.toUiText(Res.string.error_auth_change_password_failed)
                        )
                    }
                }
        }
    }

    fun unlinkApple() {
        viewModelScope.launch {
            val token = authManager.getValidAccessToken() ?: return@launch
            _state.update { it.copy(isLinkingAuth = true) }
            runCatching { authApiService.unlinkApple(token) }
                .onSuccess { response ->
                    val user = response.data?.toDomainModel()
                    if (user != null) authManager.saveUser(user)
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            user = user ?: it.user,
                            bannerMessage = UiText.StringRes(Res.string.settings_apple_unlinked)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLinkingAuth = false,
                            bannerMessage = error.toUiText(Res.string.error_auth_change_password_failed)
                        )
                    }
                }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            val cached = authManager.getUser()
            val token = authManager.getValidAccessToken()
            val user = if (token != null) {
                runCatching {
                    authApiService.getProfile(token).data?.toDomainModel()?.also { authManager.saveUser(it) }
                }.getOrNull() ?: cached
            } else {
                cached
            }
            val packs = runCatching { stickerRepository.getAllPacks() }.getOrDefault(emptyList())
            val stickersCount = packs.sumOf { it.stickers.size }
            val usage = if (user != null) {
                aiQuotaRepository.getUsage(forceRefresh = true)
            } else {
                null
            }
            val legalSummary = runCatching { legalApiRepository.getSummary() }.getOrNull()
            val unread = if (user != null) {
                runCatching {
                    exploreApiRepository.getNotifications(page = 1, limit = 1, unreadOnly = true).second
                }.getOrDefault(0)
            } else {
                0
            }
            _state.value = _state.value.copy(
                user = user,
                isLoading = false,
                stickersCount = stickersCount,
                packsCount = packs.size,
                downloadsCount = user?.totalPackDownloads ?: 0,
                aiUsage = usage,
                isLoadingAiUsage = false,
                aiUsageLoadFailed = user != null && usage == null,
                legalSummary = legalSummary,
                notificationUnreadCount = unread,
                googleAvailable = googleSignInGateway.isAvailable(),
                appleAvailable = appleSignInGateway.isAvailable()
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authSessionCoordinator.endSession()
            _state.value = _state.value.copy(
                user = null,
                stickersCount = 0,
                packsCount = 0,
                aiUsage = null,
                notificationUnreadCount = 0,
            )
        }
    }
}
