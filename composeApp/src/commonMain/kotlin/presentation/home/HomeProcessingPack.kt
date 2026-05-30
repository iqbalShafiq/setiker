package presentation.home

data class HomeProcessingPack(
    val draftId: String,
    val name: String,
    val progressFraction: Float,
    val progressLabel: String,
    val isFailed: Boolean = false,
    val failureMessage: String? = null
)
