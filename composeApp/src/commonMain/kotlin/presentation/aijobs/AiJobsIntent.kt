package presentation.aijobs

sealed interface AiJobsIntent {
    data object Load : AiJobsIntent
    data class RetryDraft(val draftId: String) : AiJobsIntent
    data class CancelJob(val jobId: String) : AiJobsIntent
    data class DeleteDraft(val draftId: String) : AiJobsIntent
    data class OpenDraft(val draftId: String) : AiJobsIntent
    data object NavigateBack : AiJobsIntent
}
