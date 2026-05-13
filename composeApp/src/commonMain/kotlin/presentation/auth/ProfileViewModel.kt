package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = true
)

class ProfileViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init { loadUser() }
    
    private fun loadUser() {
        viewModelScope.launch {
            val user = authManager.getUser()
            _state.value = ProfileState(user = user, isLoading = false)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authManager.clearTokens()
            _state.value = _state.value.copy(user = null)
        }
    }
}
