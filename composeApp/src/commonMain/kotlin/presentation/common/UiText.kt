package presentation.common

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringRes(
        val resource: StringResource,
        val args: List<Any> = emptyList()
    ) : UiText()
}

suspend fun UiText.resolve(): String {
    return when (this) {
        is UiText.DynamicString -> value
        is UiText.StringRes -> getString(resource, *args.toTypedArray())
    }
}
