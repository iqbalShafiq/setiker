package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
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
import kotlinx.coroutines.launch

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
    val notificationUnreadCount: Int = 0
)

class ProfileViewModel(
    private val authManager: AuthManager,
    private val stickerRepository: StickerRepository,
    private val aiQuotaRepository: AiQuotaRepository,
    private val legalApiRepository: LegalApiRepository,
    private val exploreApiRepository: ExploreApiRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init { loadUser() }

    fun refresh() = loadUser()
    
    private fun loadUser() {
        viewModelScope.launch {
            val user = authManager.getUser()
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
            _state.value = ProfileState(
                user = user,
                isLoading = false,
                stickersCount = stickersCount,
                packsCount = packs.size,
                downloadsCount = 0,
                aiUsage = usage,
                isLoadingAiUsage = false,
                aiUsageLoadFailed = user != null && usage == null,
                legalSummary = legalSummary,
                notificationUnreadCount = unread
            )
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authManager.clearTokens()
            _state.value = _state.value.copy(user = null)
        }
    }
}
