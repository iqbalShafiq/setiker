package presentation.aijobs

import domain.model.AiUsage
import domain.model.aijob.AiJob
import domain.model.aijob.WorkspaceDraft

data class AiJobsState(
    val isLoading: Boolean = false,
    val drafts: List<WorkspaceDraft> = emptyList(),
    val jobs: List<AiJob> = emptyList(),
    val activeJobCount: Int = 0,
    val aiUsage: AiUsage? = null,
    val isLoadingAiUsage: Boolean = false,
    val aiUsageLoadFailed: Boolean = false
)
