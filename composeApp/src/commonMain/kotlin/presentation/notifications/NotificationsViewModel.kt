package presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.ExploreApiRepository
import data.remote.model.UserNotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val isLoading: Boolean = true,
    val items: List<UserNotificationItem> = emptyList(),
    val unreadCount: Int = 0
)

class NotificationsViewModel(
    private val exploreApiRepository: ExploreApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun markRead(id: String) {
        viewModelScope.launch {
            runCatching { exploreApiRepository.markNotificationRead(id) }
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            items = current.items.map { item ->
                                if (item.id == id) item.copy(readAt = "read") else item
                            },
                            unreadCount = (current.unreadCount - 1).coerceAtLeast(0)
                        )
                    }
                }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { exploreApiRepository.markAllNotificationsRead() }
                .onSuccess {
                    _state.update { it.copy(unreadCount = 0, items = it.items.map { n -> n.copy(readAt = "read") }) }
                }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { exploreApiRepository.getNotifications(page = 1, limit = 50) }
                .onSuccess { (items, unread) ->
                    _state.update { it.copy(isLoading = false, items = items, unreadCount = unread) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false) }
                }
        }
    }
}
