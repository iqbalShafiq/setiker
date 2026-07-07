package permission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationPermissionGate {
    private val _showRationale = MutableStateFlow(false)
    val showRationale: StateFlow<Boolean> = _showRationale.asStateFlow()

    fun requestWithRationale() {
        _showRationale.value = true
    }

    fun dismissRationale() {
        _showRationale.value = false
    }
}
