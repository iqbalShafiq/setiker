package presentation.navigation

/**
 * Parsed AI notification / deep-link payload for [AppNavigation].
 */
data class NotificationDeepLink(
    val draftId: String?,
    val jobId: String?,
    val openAiJobsOnly: Boolean = false
)
