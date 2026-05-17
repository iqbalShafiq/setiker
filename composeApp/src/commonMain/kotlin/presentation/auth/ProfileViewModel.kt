package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import domain.repository.StickerRepository
import domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val stickersCount: Int = 0,
    val packsCount: Int = 0,
    val downloadsCount: Int = 0
)

class ProfileViewModel(
    private val authManager: AuthManager,
    private val stickerRepository: StickerRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init { loadUser() }
    
    private fun loadUser() {
        viewModelScope.launch {
            val user = authManager.getUser()
            val packs = runCatching { stickerRepository.getAllPacks() }.getOrDefault(emptyList())
            val stickersCount = packs.sumOf { it.stickers.size }
            _state.value = ProfileState(
                user = user,
                isLoading = false,
                stickersCount = stickersCount,
                packsCount = packs.size,
                downloadsCount = 0
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
