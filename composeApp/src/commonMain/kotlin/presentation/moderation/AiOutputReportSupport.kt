package presentation.moderation

import data.remote.ExploreApiRepository
import domain.error.Result
import domain.error.detailText

/**
 * Shared submit-latest-AI-output-report helper. Typed Result plumbing ends here: failure is
 * surfaced as an exception carrying the classified detail so the two `runCatching` call sites
 * (CreatePack/Editor) keep their existing catch-and-display behavior.
 */
object AiOutputReportSupport {
    suspend fun submitLatest(
        exploreApiRepository: ExploreApiRepository,
        reason: String,
        details: String?,
    ) {
        val historyId = when (val history = exploreApiRepository.getProcessingHistory()) {
            is Result.Error -> error(history.error.detailText())
            is Result.Success -> history.data.firstOrNull()?.id
                ?: error("No processing history found for this AI output")
        }
        when (val report = exploreApiRepository.reportProcessingHistory(historyId, reason, details)) {
            is Result.Error -> error(report.error.detailText())
            is Result.Success -> Unit
        }
    }
}
