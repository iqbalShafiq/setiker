package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PromptPreset(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val referenceHint: String? = null
)
