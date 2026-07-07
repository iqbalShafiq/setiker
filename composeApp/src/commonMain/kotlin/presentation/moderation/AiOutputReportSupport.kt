package presentation.moderation

import data.remote.ExploreApiRepository

object AiOutputReportSupport {
    suspend fun submitLatest(
        exploreApiRepository: ExploreApiRepository,
        reason: String,
        details: String?,
    ) {
        val historyId = exploreApiRepository.getProcessingHistory()
            .firstOrNull()
            ?.id
            ?: error("No processing history found for this AI output")
        exploreApiRepository.reportProcessingHistory(historyId, reason, details)
    }
}
