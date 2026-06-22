package presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.ExploreApiRepository
import data.remote.model.UserNotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val items: List<UserNotificationItem> = emptyList(),
    val unreadCount: Int = 0
)

sealed interface NotificationNavigation {
    data class PublicPack(val packId: String) : NotificationNavigation
    data class Creator(val userId: String) : NotificationNavigation
}

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
                    _state.update {
                        it.copy(unreadCount = 0, items = it.items.map { n -> n.copy(readAt = "read") })
                    }
                }
        }
    }

    fun resolveNavigation(item: UserNotificationItem): NotificationNavigation? {
        val payload = item.payload ?: return null
        payload.stringField("packId")?.let { return NotificationNavigation.PublicPack(it) }
        payload.stringField("followerId")?.let { return NotificationNavigation.Creator(it) }
        if (item.type == "FOLLOW") {
            payload.stringField("actorId")?.let { return NotificationNavigation.Creator(it) }
        }
        return null
    }

    private fun JsonObject.stringField(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false) }
            runCatching { exploreApiRepository.getNotifications(page = 1, limit = 50) }
                .onSuccess { (items, unread) ->
                    _state.update { it.copy(isLoading = false, loadFailed = false, items = items, unreadCount = unread) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }
}
