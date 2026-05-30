package presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.aijob.AiJobManager
import data.auth.AuthManager
import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.sync.SyncManager
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.model.SyncOperationStatus
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.WorkspaceDraftKind
import domain.model.aijob.WorkspaceDraftStatus
import domain.model.aijob.GeneratePackPayload
import domain.model.aijob.WorkspaceDraftContext
import domain.repository.StickerRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import presentation.aijob.AiJobEnqueueHelper
import presentation.aijob.DraftResultApplier
import presentation.aijob.WorkspaceDraftFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.PackIdentifierSanitizer
import presentation.common.toUiText
import kotlin.random.Random
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_add_pack
import setiker.composeapp.generated.resources.error_failed_delete_pack
import setiker.composeapp.generated.resources.error_failed_generate_sticker_pack
import setiker.composeapp.generated.resources.error_pack_min_stickers_whatsapp
import setiker.composeapp.generated.resources.error_pack_name_required
import setiker.composeapp.generated.resources.error_prompt_required
import setiker.composeapp.generated.resources.error_publisher_required
import setiker.composeapp.generated.resources.info_ai_job_started_background
import setiker.composeapp.generated.resources.success_generate_pack_background
import setiker.composeapp.generated.resources.success_pack_added_whatsapp

class HomeViewModel(
    private val repository: StickerRepository,
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val apiRepository: StickerApiRepository,
    private val draftSaver: StickerPackDraftSaver,
    private val aiJobManager: AiJobManager,
    private val enqueueHelper: AiJobEnqueueHelper,
    private val draftResultApplier: DraftResultApplier
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeAuthState()
        observeSyncState()
        observeActiveAiJobs()
        observeAiJobsBadge()
        observeHomeProcessingPacks()
        observeHomeGenerateJob()
    }

    private fun observeActiveAiJobs() {
        viewModelScope.launch {
            aiJobManager.observeActiveJobCount().collect { count ->
                _state.update { it.copy(activeAiJobCount = count) }
            }
        }
    }

    private fun observeAiJobsBadge() {
        viewModelScope.launch {
            aiJobManager.observeDrafts().collect { drafts ->
                val count = drafts.count { draft ->
                    draft.status != WorkspaceDraftStatus.APPLIED &&
                        draft.status != WorkspaceDraftStatus.CANCELLED
                }
                _state.update { it.copy(aiJobsBadgeCount = count) }
            }
        }
    }

    private fun observeHomeProcessingPacks() {
        viewModelScope.launch {
            combine(
                aiJobManager.observeDrafts(),
                aiJobManager.observeJobs()
            ) { drafts, jobs ->
                drafts
                    .filter { draft ->
                        draft.kind == WorkspaceDraftKind.GENERATE_PACK &&
                            draft.origin == AiJobOrigin.HOME_SHEET &&
                            (
                                draft.status == WorkspaceDraftStatus.PROCESSING ||
                                    draft.status == WorkspaceDraftStatus.NEEDS_ATTENTION
                                )
                    }
                    .map { draft ->
                        val job = jobs
                            .filter { it.workspaceDraftId == draft.id && it.type == AiJobType.GENERATE_PACK }
                            .maxByOrNull { it.updatedAt }
                        val failed = draft.status == WorkspaceDraftStatus.NEEDS_ATTENTION ||
                            job?.status in FAILED_JOB_STATUSES
                        HomeProcessingPack(
                            draftId = draft.id,
                            name = draft.displayTitle,
                            progressFraction = job?.progress?.fraction?.coerceIn(0f, 1f)
                                ?: if (failed) 0f else 0.08f,
                            progressLabel = when {
                                failed -> job?.failureMessage ?: "Gagal membuat pack"
                                !job?.progress?.stepLabel.isNullOrBlank() -> job.progress.stepLabel
                                job?.status == AiJobStatus.QUEUED -> "Antrian…"
                                else -> "Memproses di background"
                            },
                            isFailed = failed,
                            failureMessage = job?.failureMessage?.takeIf { failed }
                        )
                    }
            }.collect { processing ->
                _state.update { it.copy(processingPacks = processing) }
            }
        }
    }

    private fun observeHomeGenerateJob() {
        viewModelScope.launch {
            _state
                .map { it.homeWorkspaceDraftId }
                .distinctUntilChanged()
                .flatMapLatest { draftId ->
                    if (draftId == null) return@flatMapLatest flowOf(null)
                    combine(
                        draftResultApplier.observeDraft(draftId),
                        draftResultApplier.observeJobCompletion(draftId),
                        aiJobManager.observeJobs().map { jobs ->
                            jobs.filter { it.workspaceDraftId == draftId }.maxByOrNull { it.updatedAt }
                        }
                    ) { draft, completedJob, latestJob ->
                        Triple(draft, completedJob, latestJob)
                    }
                }
                .collect { payload ->
                    if (payload == null) return@collect
                    val (draft, completedJob, latestJob) = payload
                    if (draft == null) return@collect

                    when (latestJob?.status) {
                        AiJobStatus.FAILED_FINAL,
                        AiJobStatus.FAILED_RETRYABLE,
                        AiJobStatus.CANCELLED -> {
                            val message = latestJob.failureMessage ?: "Generate pack gagal"
                            _state.update {
                                it.copy(
                                    isGeneratePackLoading = false,
                                    homeWorkspaceDraftId = null,
                                    backgroundJobMessage = null
                                )
                            }
                            _effect.send(HomeEffect.ShowError(UiText.DynamicString(message)))
                        }
                        AiJobStatus.RUNNING,
                        AiJobStatus.QUEUED,
                        AiJobStatus.WAITING_FOR_NETWORK,
                        AiJobStatus.CHECKPOINTED -> {
                            val progress = latestJob.progress
                            if (progress != null) {
                                val percent = (progress.fraction * 100).toInt().coerceIn(0, 100)
                                _state.update {
                                    it.copy(
                                        backgroundJobMessage = "${progress.stepLabel} ($percent%)"
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }

                    val packId = draftResultApplier.applyGeneratePackCompletion(
                        _state.value,
                        draft,
                        completedJob
                    )
                    if (packId != null) {
                        loadPacks()
                        _state.update {
                            it.copy(
                                isGeneratePackLoading = false,
                                homeWorkspaceDraftId = null,
                                backgroundJobMessage = null
                            )
                        }
                        _effect.send(
                            HomeEffect.ShowSuccess(
                                UiText.StringRes(Res.string.success_generate_pack_background)
                            )
                        )
                    }
                }
        }
    }

    companion object {
        private val FAILED_JOB_STATUSES = setOf(
            AiJobStatus.FAILED_FINAL,
            AiJobStatus.FAILED_RETRYABLE,
            AiJobStatus.CANCELLED
        )
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncManager.isSyncing.collect { isSyncing ->
                _state.update { it.copy(isSyncing = isSyncing) }
            }
        }
        viewModelScope.launch {
            syncManager.operationsFlow.collect { operations ->
                val pendingCount = operations.count { it.status == SyncOperationStatus.PENDING }
                _state.update { it.copy(pendingSyncCount = pendingCount) }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadPacks -> loadPacks()
            is HomeIntent.DeletePack -> deletePack(intent.packId)
            is HomeIntent.AddToWhatsApp -> addToWhatsApp(intent.packId)
            is HomeIntent.CreateNewPack -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToCreatePack)
                }
            }
            is HomeIntent.NavigateToProfile -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToProfile)
                }
            }
            is HomeIntent.NavigateToSync -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToSync)
                }
            }
            is HomeIntent.NavigateToExplore -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToExplore)
                }
            }
            is HomeIntent.NavigateToLogin -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToLogin)
                }
            }
            is HomeIntent.RefreshSync -> {
                viewModelScope.launch {
                    syncManager.sync()
                }
            }
            is HomeIntent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is HomeIntent.SortOrderChanged -> {
                _state.update { it.copy(sortOrder = intent.order) }
            }
            is HomeIntent.OpenGeneratePackSheet -> {
                _state.update { it.copy(isGeneratePackSheetOpen = true) }
            }
            is HomeIntent.CloseGeneratePackSheet -> {
                _state.update { it.copy(isGeneratePackSheetOpen = false) }
            }
            is HomeIntent.UpdateGeneratePackPrompt -> {
                _state.update { it.copy(generatePackPrompt = intent.prompt) }
            }
            is HomeIntent.UpdateGeneratePackName -> {
                _state.update { it.copy(generatePackName = intent.name) }
            }
            is HomeIntent.UpdateGeneratePackPublisher -> {
                _state.update { it.copy(generatePackPublisher = intent.publisher) }
            }
            is HomeIntent.UpdateGeneratePackLayout -> {
                _state.update { it.copy(generatePackLayout = intent.layout) }
            }
            is HomeIntent.UpdateGeneratePackInputImage -> {
                _state.update { it.copy(generatePackInputImagePath = intent.path) }
            }
            is HomeIntent.GenerateStickerPack -> generateStickerPack()
            is HomeIntent.StartVideoStickerPack -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToVideoStickerPack(intent.videoPath))
                }
            }
            HomeIntent.NavigateToAiJobs -> {
                viewModelScope.launch { _effect.send(HomeEffect.NavigateToAiJobs) }
            }
        }
    }

    private fun loadPacks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val packs = repository.getAllPacks()
                _state.update { it.copy(isLoading = false, packs = packs) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun deletePack(packId: String) {
        viewModelScope.launch {
            try {
                repository.deletePack(packId)
                loadPacks()
            } catch (e: Exception) {
                _effect.send(
                    HomeEffect.ShowError(
                        e.toUiText(Res.string.error_failed_delete_pack)
                    )
                )
            }
        }
    }

    private fun addToWhatsApp(packId: String) {
        viewModelScope.launch {
            try {
                val pack = repository.getPack(packId)
                if (pack.stickers.size < StickerPack.MIN_STICKERS) {
                    _effect.send(
                        HomeEffect.ShowError(
                            UiText.StringRes(
                                Res.string.error_pack_min_stickers_whatsapp,
                                listOf(StickerPack.MIN_STICKERS)
                            )
                        )
                    )
                    return@launch
                }
                _effect.send(HomeEffect.ShowSuccess(UiText.StringRes(Res.string.success_pack_added_whatsapp)))
            } catch (e: Exception) {
                _effect.send(
                    HomeEffect.ShowError(
                        e.toUiText(Res.string.error_failed_add_pack)
                    )
                )
            }
        }
    }

    private fun generateStickerPack() {
        viewModelScope.launch {
            val current = _state.value
            if (current.isGeneratePackLoading || current.homeWorkspaceDraftId != null) return@launch
            if (current.generatePackName.isBlank()) {
                _effect.send(HomeEffect.ShowError(UiText.StringRes(Res.string.error_pack_name_required)))
                return@launch
            }
            if (current.generatePackPublisher.isBlank()) {
                _effect.send(HomeEffect.ShowError(UiText.StringRes(Res.string.error_publisher_required)))
                return@launch
            }
            if (current.generatePackPrompt.isBlank()) {
                _effect.send(HomeEffect.ShowError(UiText.StringRes(Res.string.error_prompt_required)))
                return@launch
            }

            val context = WorkspaceDraftContext(
                prompt = current.generatePackPrompt,
                packName = current.generatePackName,
                publisher = current.generatePackPublisher,
                layout = current.generatePackLayout,
                inputImagePath = current.generatePackInputImagePath,
                homeAutoSaveOnComplete = true
            )
            val draft = WorkspaceDraftFactory.create(
                kind = WorkspaceDraftKind.GENERATE_PACK,
                origin = AiJobOrigin.HOME_SHEET,
                displayTitle = current.generatePackName.ifBlank { "Sticker pack" },
                context = context,
                originRoute = "home"
            )
            aiJobManager.upsertDraft(draft)
            enqueueHelper.enqueueGeneratePack(
                draft = draft,
                payload = GeneratePackPayload(
                    prompt = current.generatePackPrompt,
                    layout = current.generatePackLayout,
                    packName = current.generatePackName,
                    publisher = current.generatePackPublisher,
                    inputImagePath = current.generatePackInputImagePath
                )
            )
            _state.update {
                it.copy(
                    isGeneratePackLoading = true,
                    error = null,
                    homeWorkspaceDraftId = draft.id,
                    backgroundJobMessage = "Sedang diproses di background"
                )
            }
            _effect.send(
                HomeEffect.ShowSuccess(UiText.StringRes(Res.string.info_ai_job_started_background))
            )
        }
    }
}
