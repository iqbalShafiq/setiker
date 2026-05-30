package presentation.aijobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.aijob.AiJobManager
import domain.model.aijob.WorkspaceDraftStatus
import domain.repository.AiJobRepository
import domain.repository.WorkspaceDraftRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiJobsViewModel(
    private val draftRepository: WorkspaceDraftRepository,
    private val jobRepository: AiJobRepository,
    private val aiJobManager: AiJobManager
) : ViewModel() {
    private val _state = MutableStateFlow(AiJobsState())
    val state: StateFlow<AiJobsState> = _state.asStateFlow()

    private val _effect = Channel<AiJobsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                draftRepository.observeAll(),
                jobRepository.observeAll(),
                jobRepository.observeActiveCount()
            ) { drafts, jobs, activeCount ->
                Triple(drafts, jobs, activeCount)
            }.collect { (drafts, jobs, activeCount) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        drafts = drafts,
                        jobs = jobs,
                        activeJobCount = activeCount
                    )
                }
            }
        }
    }

    fun onIntent(intent: AiJobsIntent) {
        when (intent) {
            AiJobsIntent.Load -> Unit
            AiJobsIntent.NavigateBack -> viewModelScope.launch { _effect.send(AiJobsEffect.NavigateBack) }
            is AiJobsIntent.RetryDraft -> retryDraft(intent.draftId)
            is AiJobsIntent.CancelJob -> viewModelScope.launch {
                aiJobManager.cancelJob(intent.jobId)
            }
            is AiJobsIntent.DeleteDraft -> viewModelScope.launch {
                aiJobManager.deleteDraft(intent.draftId)
            }
            is AiJobsIntent.OpenDraft -> openDraft(intent.draftId)
        }
    }

    private fun retryDraft(draftId: String) {
        viewModelScope.launch {
            val job = aiJobManager.retryDraft(draftId)
            if (job == null) {
                _effect.send(AiJobsEffect.ShowMessage("Unable to retry this draft"))
            }
        }
    }

    private fun openDraft(draftId: String) {
        viewModelScope.launch {
            val draft = draftRepository.getById(draftId) ?: return@launch
            when (draft.status) {
                WorkspaceDraftStatus.APPLIED -> {
                    val packId = presentation.aijob.WorkspaceDraftFactory
                        .decodeContext(draft)
                        .resultPackId
                    if (!packId.isNullOrBlank()) {
                        _effect.send(AiJobsEffect.NavigateToPack(packId))
                    } else {
                        _effect.send(
                            AiJobsEffect.NavigateToDraft(draftId, draft.originRoute)
                        )
                    }
                }
                else -> _effect.send(
                    AiJobsEffect.NavigateToDraft(draftId, draft.originRoute)
                )
            }
        }
    }
}
